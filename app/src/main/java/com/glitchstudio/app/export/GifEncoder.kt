package com.glitchstudio.app.export

import android.graphics.Bitmap
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.io.OutputStream

/**
 * Animated GIF89a encoder. Kotlin port of Kevin Weiner's public-domain
 * AnimatedGifEncoder, adapted to consume Android [Bitmap]s. Uses [NeuQuant] for
 * colour quantisation and [LzwEncoder] for the image data.
 */
class GifEncoder {

    private var width = 0
    private var height = 0
    private var transIndex = 0
    private var repeat = 0          // -1 = no repeat, 0 = loop forever
    private var delay = 0           // frame delay in 1/100 sec
    private var started = false
    private var out: OutputStream? = null
    private var pixels: ByteArray? = null        // BGR byte array of current frame
    private var indexedPixels: ByteArray? = null // palette index per pixel
    private var colorDepth = 0
    private var colorTab: ByteArray? = null
    private val usedEntry = BooleanArray(256)
    private val palSize = 7
    private var dispose = -1
    private var firstFrame = true
    private var sample = 10

    /** Frame delay in milliseconds. */
    fun setDelay(ms: Int) { delay = Math.round(ms / 10f) }

    /** 0 = loop forever (default), n = number of repeats. */
    fun setRepeat(iter: Int) { if (iter >= 0) repeat = iter }

    /** 1 = best quality (slowest), 10..20 = good/fast. */
    fun setQuality(quality: Int) { sample = if (quality < 1) 1 else quality }

    fun start(os: OutputStream): Boolean {
        var ok = true
        out = os
        try {
            writeString("GIF89a")
        } catch (e: IOException) {
            ok = false
        }
        started = ok
        return ok
    }

    fun addFrame(bm: Bitmap): Boolean {
        if (!started || out == null) return false
        var ok = true
        try {
            width = bm.width
            height = bm.height
            getImagePixels(bm)
            analyzePixels()
            if (firstFrame) {
                writeLSD()
                writePalette()
                if (repeat >= 0) writeNetscapeExt()
            }
            writeGraphicCtrlExt()
            writeImageDesc()
            if (!firstFrame) writePalette()
            writePixels()
            firstFrame = false
        } catch (e: IOException) {
            ok = false
        }
        return ok
    }

    fun finish(): Boolean {
        if (!started) return false
        var ok = true
        started = false
        try {
            out!!.write(0x3b) // gif trailer
            out!!.flush()
        } catch (e: IOException) {
            ok = false
        }
        // reset
        out = null
        pixels = null
        indexedPixels = null
        colorTab = null
        firstFrame = true
        return ok
    }

    private fun getImagePixels(bm: Bitmap) {
        val w = bm.width
        val h = bm.height
        val px = IntArray(w * h)
        bm.getPixels(px, 0, w, 0, 0, w, h)
        val p = ByteArray(w * h * 3)
        var bi = 0
        for (pix in px) {
            p[bi++] = (pix and 0xff).toByte()          // blue
            p[bi++] = ((pix shr 8) and 0xff).toByte()  // green
            p[bi++] = ((pix shr 16) and 0xff).toByte() // red
        }
        pixels = p
    }

    private fun analyzePixels() {
        val data = pixels!!
        val nPix = data.size / 3
        indexedPixels = ByteArray(nPix)
        val nq = NeuQuant(data, data.size, sample)
        val tab = nq.process()
        // NeuQuant returns BGR; swap to RGB for the GIF palette.
        var i = 0
        while (i < tab.size) {
            val temp = tab[i]
            tab[i] = tab[i + 2]
            tab[i + 2] = temp
            usedEntry[i / 3] = false
            i += 3
        }
        colorTab = tab
        var k = 0
        val idx = indexedPixels!!
        for (j in 0 until nPix) {
            val index = nq.map(
                data[k++].toInt() and 0xff,
                data[k++].toInt() and 0xff,
                data[k++].toInt() and 0xff,
            )
            usedEntry[index] = true
            idx[j] = index.toByte()
        }
        pixels = null
        colorDepth = 8
        transIndex = 0
    }

    private fun writeLSD() {
        writeShort(width)
        writeShort(height)
        out!!.write(0x80 or 0x70 or palSize) // gct flag, color res, gct size
        out!!.write(0) // background color index
        out!!.write(0) // pixel aspect ratio
    }

    private fun writePalette() {
        val tab = colorTab!!
        out!!.write(tab, 0, tab.size)
        val n = (3 * 256) - tab.size
        for (i in 0 until n) out!!.write(0)
    }

    private fun writeNetscapeExt() {
        out!!.write(0x21)
        out!!.write(0xff)
        out!!.write(11)
        writeString("NETSCAPE2.0")
        out!!.write(3)
        out!!.write(1)
        writeShort(repeat)
        out!!.write(0)
    }

    private fun writeGraphicCtrlExt() {
        out!!.write(0x21)
        out!!.write(0xf9)
        out!!.write(4)
        var disp = 0
        if (dispose >= 0) disp = dispose and 7
        disp = disp shl 2
        out!!.write(disp)
        writeShort(delay)
        out!!.write(transIndex)
        out!!.write(0)
    }

    private fun writeImageDesc() {
        out!!.write(0x2c)
        writeShort(0)
        writeShort(0)
        writeShort(width)
        writeShort(height)
        if (firstFrame) out!!.write(0) else out!!.write(0x80 or palSize)
    }

    private fun writePixels() {
        val enc = LzwEncoder(width, height, indexedPixels!!, colorDepth)
        enc.encode(out!!)
    }

    private fun writeShort(value: Int) {
        out!!.write(value and 0xff)
        out!!.write((value shr 8) and 0xff)
    }

    private fun writeString(s: String) {
        for (c in s) out!!.write(c.code)
    }
}

/**
 * Neural-net colour quantiser (Anthony Dekker's NeuQuant, via Kevin Weiner).
 * Produces a 256-entry palette and maps colours to it.
 */
internal class NeuQuant(
    private val thepicture: ByteArray,
    private val lengthcount: Int,
    sample: Int,
) {
    private val netsize = 256
    private val prime1 = 499
    private val prime2 = 491
    private val prime3 = 487
    private val prime4 = 503
    private val minpicturebytes = 3 * prime4
    private val maxnetpos = netsize - 1
    private val netbiasshift = 4
    private val ncycles = 100
    private val intbiasshift = 16
    private val intbias = 1 shl intbiasshift
    private val gammashift = 10
    private val betashift = 10
    private val beta = intbias shr betashift
    private val betagamma = intbias shl (gammashift - betashift)
    private val initrad = netsize shr 3
    private val radiusbiasshift = 6
    private val radiusbias = 1 shl radiusbiasshift
    private val initradius = initrad * radiusbias
    private val radiusdec = 30
    private val alphabiasshift = 10
    private val initalpha = 1 shl alphabiasshift
    private var alphadec = 0
    private val radbiasshift = 8
    private val radbias = 1 shl radbiasshift
    private val alpharadbshift = alphabiasshift + radbiasshift
    private val alpharadbias = 1 shl alpharadbshift

    private var samplefac = sample
    private val network = Array(netsize) { IntArray(4) }
    private val netindex = IntArray(256)
    private val bias = IntArray(netsize)
    private val freq = IntArray(netsize)
    private val radpower = IntArray(initrad)

    init {
        for (i in 0 until netsize) {
            val p = network[i]
            val v = (i shl (netbiasshift + 8)) / netsize
            p[0] = v; p[1] = v; p[2] = v
            freq[i] = intbias / netsize
            bias[i] = 0
        }
    }

    fun process(): ByteArray {
        learn()
        unbiasnet()
        inxbuild()
        return colorMap()
    }

    private fun colorMap(): ByteArray {
        val map = ByteArray(3 * netsize)
        val index = IntArray(netsize)
        for (i in 0 until netsize) index[network[i][3]] = i
        var k = 0
        for (i in 0 until netsize) {
            val j = index[i]
            map[k++] = network[j][0].toByte()
            map[k++] = network[j][1].toByte()
            map[k++] = network[j][2].toByte()
        }
        return map
    }

    private fun unbiasnet() {
        for (i in 0 until netsize) {
            network[i][0] = network[i][0] shr netbiasshift
            network[i][1] = network[i][1] shr netbiasshift
            network[i][2] = network[i][2] shr netbiasshift
            network[i][3] = i
        }
    }

    private fun inxbuild() {
        var previouscol = 0
        var startpos = 0
        for (i in 0 until netsize) {
            val p = network[i]
            var smallpos = i
            var smallval = p[1]
            for (j in i + 1 until netsize) {
                val q = network[j]
                if (q[1] < smallval) { smallpos = j; smallval = q[1] }
            }
            val q = network[smallpos]
            if (i != smallpos) {
                var t = q[0]; q[0] = p[0]; p[0] = t
                t = q[1]; q[1] = p[1]; p[1] = t
                t = q[2]; q[2] = p[2]; p[2] = t
                t = q[3]; q[3] = p[3]; p[3] = t
            }
            if (smallval != previouscol) {
                netindex[previouscol] = (startpos + i) shr 1
                for (j in previouscol + 1 until smallval) netindex[j] = i
                previouscol = smallval
                startpos = i
            }
        }
        netindex[previouscol] = (startpos + maxnetpos) shr 1
        for (j in previouscol + 1 until 256) netindex[j] = maxnetpos
    }

    private fun learn() {
        if (lengthcount < minpicturebytes) samplefac = 1
        alphadec = 30 + ((samplefac - 1) / 3)
        val p = thepicture
        var pix = 0
        val lim = lengthcount
        val samplepixels = lengthcount / (3 * samplefac)
        var delta = samplepixels / ncycles
        var alpha = initalpha
        var radius = initradius
        var rad = radius shr radiusbiasshift
        if (rad <= 1) rad = 0
        for (i in 0 until rad) radpower[i] = alpha * (((rad * rad - i * i) * radbias) / (rad * rad))

        val step: Int = when {
            lengthcount < minpicturebytes -> 3
            lengthcount % prime1 != 0 -> 3 * prime1
            lengthcount % prime2 != 0 -> 3 * prime2
            lengthcount % prime3 != 0 -> 3 * prime3
            else -> 3 * prime4
        }

        var i = 0
        if (delta == 0) delta = 1
        while (i < samplepixels) {
            val b = (p[pix].toInt() and 0xff) shl netbiasshift
            val g = (p[pix + 1].toInt() and 0xff) shl netbiasshift
            val r = (p[pix + 2].toInt() and 0xff) shl netbiasshift
            val j = contest(b, g, r)
            altersingle(alpha, j, b, g, r)
            if (rad != 0) alterneigh(rad, j, b, g, r)
            pix += step
            if (pix >= lim) pix -= lengthcount
            i++
            if (i % delta == 0) {
                alpha -= alpha / alphadec
                radius -= radius / radiusdec
                rad = radius shr radiusbiasshift
                if (rad <= 1) rad = 0
                for (k in 0 until rad) radpower[k] = alpha * (((rad * rad - k * k) * radbias) / (rad * rad))
            }
        }
    }

    fun map(b: Int, g: Int, r: Int): Int {
        var bestd = 1000
        var best = -1
        var i = netindex[g]
        var j = i - 1
        while (i < netsize || j >= 0) {
            if (i < netsize) {
                val p = network[i]
                var dist = p[1] - g
                if (dist >= bestd) {
                    i = netsize
                } else {
                    i++
                    if (dist < 0) dist = -dist
                    var a = p[0] - b
                    if (a < 0) a = -a
                    dist += a
                    if (dist < bestd) {
                        a = p[2] - r
                        if (a < 0) a = -a
                        dist += a
                        if (dist < bestd) { bestd = dist; best = p[3] }
                    }
                }
            }
            if (j >= 0) {
                val p = network[j]
                var dist = g - p[1]
                if (dist >= bestd) {
                    j = -1
                } else {
                    j--
                    if (dist < 0) dist = -dist
                    var a = p[0] - b
                    if (a < 0) a = -a
                    dist += a
                    if (dist < bestd) {
                        a = p[2] - r
                        if (a < 0) a = -a
                        dist += a
                        if (dist < bestd) { bestd = dist; best = p[3] }
                    }
                }
            }
        }
        return best
    }

    private fun altersingle(alpha: Int, i: Int, b: Int, g: Int, r: Int) {
        val n = network[i]
        n[0] -= (alpha * (n[0] - b)) / initalpha
        n[1] -= (alpha * (n[1] - g)) / initalpha
        n[2] -= (alpha * (n[2] - r)) / initalpha
    }

    private fun alterneigh(rad: Int, i: Int, b: Int, g: Int, r: Int) {
        var lo = i - rad
        if (lo < -1) lo = -1
        var hi = i + rad
        if (hi > netsize) hi = netsize
        var j = i + 1
        var k = i - 1
        var m = 1
        while (j < hi || k > lo) {
            if (m >= radpower.size) break
            val a = radpower[m++]
            if (j < hi) {
                val p = network[j++]
                p[0] -= (a * (p[0] - b)) / alpharadbias
                p[1] -= (a * (p[1] - g)) / alpharadbias
                p[2] -= (a * (p[2] - r)) / alpharadbias
            }
            if (k > lo) {
                val p = network[k--]
                p[0] -= (a * (p[0] - b)) / alpharadbias
                p[1] -= (a * (p[1] - g)) / alpharadbias
                p[2] -= (a * (p[2] - r)) / alpharadbias
            }
        }
    }

    private fun contest(b: Int, g: Int, r: Int): Int {
        var bestd = Int.MAX_VALUE
        var bestbiasd = bestd
        var bestpos = -1
        var bestbiaspos = -1
        for (i in 0 until netsize) {
            val n = network[i]
            var dist = n[0] - b
            if (dist < 0) dist = -dist
            var a = n[1] - g
            if (a < 0) a = -a
            dist += a
            a = n[2] - r
            if (a < 0) a = -a
            dist += a
            if (dist < bestd) { bestd = dist; bestpos = i }
            val biasdist = dist - (bias[i] shr (intbiasshift - netbiasshift))
            if (biasdist < bestbiasd) { bestbiasd = biasdist; bestbiaspos = i }
            val betafreq = freq[i] shr betashift
            freq[i] -= betafreq
            bias[i] += betafreq shl gammashift
        }
        freq[bestpos] += beta
        bias[bestpos] -= betagamma
        return bestbiaspos
    }
}

/** LZW encoder for GIF image data (Kevin Weiner / public domain). */
internal class LzwEncoder(
    private val imgW: Int,
    private val imgH: Int,
    private val pixAry: ByteArray,
    colorDepth: Int,
) {
    private val eof = -1
    private val initCodeSize = maxOf(2, colorDepth)
    private var remaining = 0
    private var curPixel = 0

    private val bits = 12
    private val hsize = 5003
    private var nBits = 0
    private val maxbits = bits
    private var maxcode = 0
    private val maxmaxcode = 1 shl bits
    private val htab = IntArray(hsize)
    private val codetab = IntArray(hsize)
    private var freeEnt = 0
    private var clearFlg = false
    private var gInitBits = 0
    private var clearCode = 0
    private var eofCode = 0
    private var curAccum = 0
    private var curBits = 0
    private val masks = intArrayOf(
        0x0000, 0x0001, 0x0003, 0x0007, 0x000F, 0x001F, 0x003F, 0x007F,
        0x00FF, 0x01FF, 0x03FF, 0x07FF, 0x0FFF, 0x1FFF, 0x3FFF, 0x7FFF, 0xFFFF,
    )
    private var aCount = 0
    private val accum = ByteArray(256)

    fun encode(os: OutputStream) {
        os.write(initCodeSize)
        remaining = imgW * imgH
        curPixel = 0
        compress(initCodeSize + 1, os)
        os.write(0) // block terminator
    }

    private fun maxcode(nBits: Int): Int = (1 shl nBits) - 1

    private fun nextPixel(): Int {
        if (remaining == 0) return eof
        remaining--
        return pixAry[curPixel++].toInt() and 0xff
    }

    private fun compress(initBits: Int, outs: OutputStream) {
        gInitBits = initBits
        clearFlg = false
        nBits = gInitBits
        maxcode = maxcode(nBits)
        clearCode = 1 shl (initBits - 1)
        eofCode = clearCode + 1
        freeEnt = clearCode + 2
        aCount = 0
        var ent = nextPixel()
        var hshift = 0
        var fcode = hsize
        while (fcode < 65536) { hshift++; fcode *= 2 }
        hshift = 8 - hshift
        cl_hash(hsize)
        output(clearCode, outs)

        outer@ while (true) {
            val c = nextPixel()
            if (c == eof) break
            fcode = (c shl maxbits) + ent
            var i = (c shl hshift) xor ent
            if (htab[i] == fcode) { ent = codetab[i]; continue }
            if (htab[i] >= 0) {
                var disp = hsize - i
                if (i == 0) disp = 1
                do {
                    i -= disp
                    if (i < 0) i += hsize
                    if (htab[i] == fcode) { ent = codetab[i]; continue@outer }
                } while (htab[i] >= 0)
            }
            output(ent, outs)
            ent = c
            if (freeEnt < maxmaxcode) {
                codetab[i] = freeEnt++
                htab[i] = fcode
            } else {
                cl_block(outs)
            }
        }
        output(ent, outs)
        output(eofCode, outs)
    }

    private fun output(code: Int, outs: OutputStream) {
        curAccum = curAccum and masks[curBits]
        curAccum = if (curBits > 0) curAccum or (code shl curBits) else code
        curBits += nBits
        while (curBits >= 8) {
            charOut((curAccum and 0xff).toByte(), outs)
            curAccum = curAccum shr 8
            curBits -= 8
        }
        if (freeEnt > maxcode || clearFlg) {
            if (clearFlg) {
                nBits = gInitBits
                maxcode = maxcode(nBits)
                clearFlg = false
            } else {
                nBits++
                maxcode = if (nBits == maxbits) maxmaxcode else maxcode(nBits)
            }
        }
        if (code == eofCode) {
            while (curBits > 0) {
                charOut((curAccum and 0xff).toByte(), outs)
                curAccum = curAccum shr 8
                curBits -= 8
            }
            flushChar(outs)
        }
    }

    private fun cl_block(outs: OutputStream) {
        cl_hash(hsize)
        freeEnt = clearCode + 2
        clearFlg = true
        output(clearCode, outs)
    }

    private fun cl_hash(hsize: Int) {
        for (i in 0 until hsize) htab[i] = -1
    }

    private fun charOut(c: Byte, outs: OutputStream) {
        accum[aCount++] = c
        if (aCount >= 254) flushChar(outs)
    }

    private fun flushChar(outs: OutputStream) {
        if (aCount > 0) {
            outs.write(aCount)
            outs.write(accum, 0, aCount)
            aCount = 0
        }
    }
}
