package com.glitchstudio.app.export

import android.graphics.Bitmap
import java.io.ByteArrayOutputStream
import java.io.OutputStream

/**
 * Minimal, dependency-free animated GIF89a encoder.
 *
 * Colour reduction uses the NeuQuant neural-net quantiser and image data is
 * LZW-compressed. Both are faithful Kotlin ports of Anthony Dekker's NeuQuant
 * and the classic GIF LZW encoder (public domain), wired up here to emit a
 * looping GIF from a list of [Bitmap] frames.
 */
object GifEncoder {

    /**
     * @param frames  the animation frames (all assumed the same size)
     * @param delayMs per-frame delay in milliseconds
     * @param repeat  loop count, 0 = infinite
     * @param sample  NeuQuant sampling factor (1 = best/slowest, 10 = fast)
     */
    fun encode(
        frames: List<Bitmap>,
        delayMs: Int,
        repeat: Int = 0,
        sample: Int = 10
    ): ByteArray {
        val os = ByteArrayOutputStream()
        writeString(os, "GIF89a")
        var first = true
        for (frame in frames) {
            val w = frame.width
            val h = frame.height
            val pixels = bgrPixels(frame)
            val nq = NeuQuant(pixels, pixels.size, sample.coerceAtLeast(1))
            val colorTab = nq.process()
            // NeuQuant emits BGR; GIF wants RGB.
            var j = 0
            while (j < colorTab.size) {
                val t = colorTab[j]; colorTab[j] = colorTab[j + 2]; colorTab[j + 2] = t
                j += 3
            }
            val indexed = ByteArray(w * h)
            var k = 0
            for (px in 0 until w * h) {
                val b = pixels[k++].toInt() and 0xff
                val g = pixels[k++].toInt() and 0xff
                val r = pixels[k++].toInt() and 0xff
                indexed[px] = nq.map(b, g, r).toByte()
            }
            if (first) {
                writeLSD(os, w, h)
                writeNetscapeExt(os, repeat)
                first = false
            }
            writeGraphicCtrlExt(os, delayMs)
            writeImageDesc(os, w, h)
            writePalette(os, colorTab)
            LZWEncoder(w, h, indexed, 8).encode(os)
        }
        os.write(0x3B) // trailer
        return os.toByteArray()
    }

    private fun bgrPixels(bmp: Bitmap): ByteArray {
        val w = bmp.width
        val h = bmp.height
        val pix = IntArray(w * h)
        bmp.getPixels(pix, 0, w, 0, 0, w, h)
        val out = ByteArray(w * h * 3)
        var b = 0
        for (c in pix) {
            out[b++] = (c and 0xff).toByte()          // blue
            out[b++] = ((c shr 8) and 0xff).toByte()  // green
            out[b++] = ((c shr 16) and 0xff).toByte() // red
        }
        return out
    }

    private fun writeString(os: OutputStream, s: String) {
        for (ch in s) os.write(ch.code)
    }

    private fun writeShort(os: OutputStream, v: Int) {
        os.write(v and 0xff)
        os.write((v shr 8) and 0xff)
    }

    private fun writeLSD(os: OutputStream, w: Int, h: Int) {
        writeShort(os, w)
        writeShort(os, h)
        os.write(0x70) // no global colour table; colour resolution = 7
        os.write(0)    // background colour index
        os.write(0)    // pixel aspect ratio
    }

    private fun writeNetscapeExt(os: OutputStream, repeat: Int) {
        os.write(0x21)
        os.write(0xFF)
        os.write(11)
        writeString(os, "NETSCAPE2.0")
        os.write(3)
        os.write(1)
        writeShort(os, repeat)
        os.write(0)
    }

    private fun writeGraphicCtrlExt(os: OutputStream, delayMs: Int) {
        os.write(0x21)
        os.write(0xF9)
        os.write(4)
        os.write(0) // no transparency, disposal = 0
        writeShort(os, delayMs / 10) // delay in 1/100 sec
        os.write(0) // transparent colour index
        os.write(0) // block terminator
    }

    private fun writeImageDesc(os: OutputStream, w: Int, h: Int) {
        os.write(0x2C)
        writeShort(os, 0)
        writeShort(os, 0)
        writeShort(os, w)
        writeShort(os, h)
        os.write(0x87) // local colour table flag + size (256 entries)
    }

    private fun writePalette(os: OutputStream, colorTab: ByteArray) {
        os.write(colorTab, 0, colorTab.size)
        val pad = 768 - colorTab.size
        for (i in 0 until pad) os.write(0)
    }
}

// ---------------------------------------------------------------------------
// NeuQuant Neural-Net image quantization algorithm (Anthony Dekker, 1994).
// Public domain. Ported to Kotlin.
// ---------------------------------------------------------------------------
private class NeuQuant(
    private val thepicture: ByteArray,
    private val lengthcount: Int,
    private var samplefac: Int
) {
    private val network = Array(NETSIZE) { i ->
        val p = IntArray(4)
        val v = (i shl (NETBIASSHIFT + 8)) / NETSIZE
        p[0] = v; p[1] = v; p[2] = v
        p
    }
    private val netindex = IntArray(256)
    private val bias = IntArray(NETSIZE)
    private val freq = IntArray(NETSIZE) { INTBIAS / NETSIZE }
    private val radpower = IntArray(INITRAD)
    private var alphadec = 0

    fun process(): ByteArray {
        learn()
        unbiasnet()
        inxbuild()
        return colorMap()
    }

    private fun colorMap(): ByteArray {
        val map = ByteArray(3 * NETSIZE)
        val index = IntArray(NETSIZE)
        for (i in 0 until NETSIZE) index[network[i][3]] = i
        var k = 0
        for (i in 0 until NETSIZE) {
            val j = index[i]
            map[k++] = network[j][0].toByte()
            map[k++] = network[j][1].toByte()
            map[k++] = network[j][2].toByte()
        }
        return map
    }

    private fun inxbuild() {
        var previouscol = 0
        var startpos = 0
        for (i in 0 until NETSIZE) {
            val p = network[i]
            var smallpos = i
            var smallval = p[1]
            for (j in i + 1 until NETSIZE) {
                val q = network[j]
                if (q[1] < smallval) { smallpos = j; smallval = q[1] }
            }
            val q = network[smallpos]
            if (i != smallpos) {
                for (t in 0 until 4) { val tmp = q[t]; q[t] = p[t]; p[t] = tmp }
            }
            if (smallval != previouscol) {
                netindex[previouscol] = (startpos + i) shr 1
                for (j in previouscol + 1 until smallval) netindex[j] = i
                previouscol = smallval
                startpos = i
            }
        }
        netindex[previouscol] = (startpos + MAXNETPOS) shr 1
        for (j in previouscol + 1 until 256) netindex[j] = MAXNETPOS
    }

    fun map(b: Int, g: Int, r: Int): Int {
        var bestd = 1000
        var best = -1
        var i = netindex[g]
        var j = i - 1
        while (i < NETSIZE || j >= 0) {
            if (i < NETSIZE) {
                val p = network[i]
                var dist = p[1] - g
                if (dist >= bestd) {
                    i = NETSIZE
                } else {
                    i++
                    if (dist < 0) dist = -dist
                    var a = p[0] - b; if (a < 0) a = -a; dist += a
                    if (dist < bestd) {
                        a = p[2] - r; if (a < 0) a = -a; dist += a
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
                    var a = p[0] - b; if (a < 0) a = -a; dist += a
                    if (dist < bestd) {
                        a = p[2] - r; if (a < 0) a = -a; dist += a
                        if (dist < bestd) { bestd = dist; best = p[3] }
                    }
                }
            }
        }
        return best
    }

    private fun unbiasnet() {
        for (i in 0 until NETSIZE) {
            network[i][0] = network[i][0] shr NETBIASSHIFT
            network[i][1] = network[i][1] shr NETBIASSHIFT
            network[i][2] = network[i][2] shr NETBIASSHIFT
            network[i][3] = i
        }
    }

    private fun alterneigh(rad: Int, i: Int, b: Int, g: Int, r: Int) {
        var lo = i - rad; if (lo < -1) lo = -1
        var hi = i + rad; if (hi > NETSIZE) hi = NETSIZE
        var j = i + 1
        var k = i - 1
        var m = 1
        while (j < hi || k > lo) {
            val a = radpower[m++]
            if (j < hi) {
                val p = network[j++]
                p[0] -= (a * (p[0] - b)) / ALPHARADBIAS
                p[1] -= (a * (p[1] - g)) / ALPHARADBIAS
                p[2] -= (a * (p[2] - r)) / ALPHARADBIAS
            }
            if (k > lo) {
                val p = network[k--]
                p[0] -= (a * (p[0] - b)) / ALPHARADBIAS
                p[1] -= (a * (p[1] - g)) / ALPHARADBIAS
                p[2] -= (a * (p[2] - r)) / ALPHARADBIAS
            }
        }
    }

    private fun altersingle(alpha: Int, i: Int, b: Int, g: Int, r: Int) {
        val n = network[i]
        n[0] -= (alpha * (n[0] - b)) / INITALPHA
        n[1] -= (alpha * (n[1] - g)) / INITALPHA
        n[2] -= (alpha * (n[2] - r)) / INITALPHA
    }

    private fun contest(b: Int, g: Int, r: Int): Int {
        var bestd = Int.MAX_VALUE
        var bestbiasd = bestd
        var bestpos = -1
        var bestbiaspos = -1
        for (i in 0 until NETSIZE) {
            val n = network[i]
            var dist = n[0] - b; if (dist < 0) dist = -dist
            var a = n[1] - g; if (a < 0) a = -a; dist += a
            a = n[2] - r; if (a < 0) a = -a; dist += a
            if (dist < bestd) { bestd = dist; bestpos = i }
            val biasdist = dist - (bias[i] shr (INTBIASSHIFT - NETBIASSHIFT))
            if (biasdist < bestbiasd) { bestbiasd = biasdist; bestbiaspos = i }
            val betafreq = freq[i] shr BETASHIFT
            freq[i] -= betafreq
            bias[i] += betafreq shl GAMMASHIFT
        }
        freq[bestpos] += BETA
        bias[bestpos] -= BETAGAMMA
        return bestbiaspos
    }

    private fun learn() {
        if (lengthcount < MINPICTUREBYTES) samplefac = 1
        alphadec = 30 + ((samplefac - 1) / 3)
        val p = thepicture
        var pix = 0
        val lim = lengthcount
        val samplepixels = lengthcount / (3 * samplefac)
        var delta = samplepixels / NCYCLES
        var alpha = INITALPHA
        var radius = INITRADIUS
        var rad = radius shr RADIUSBIASSHIFT
        if (rad <= 1) rad = 0
        for (i in 0 until rad) radpower[i] = alpha * (((rad * rad - i * i) * RADBIAS) / (rad * rad))

        val step = when {
            lengthcount < MINPICTUREBYTES -> 3
            lengthcount % PRIME1 != 0 -> 3 * PRIME1
            lengthcount % PRIME2 != 0 -> 3 * PRIME2
            lengthcount % PRIME3 != 0 -> 3 * PRIME3
            else -> 3 * PRIME4
        }

        var i = 0
        if (delta == 0) delta = 1
        while (i < samplepixels) {
            val b = (p[pix].toInt() and 0xff) shl NETBIASSHIFT
            val g = (p[pix + 1].toInt() and 0xff) shl NETBIASSHIFT
            val r = (p[pix + 2].toInt() and 0xff) shl NETBIASSHIFT
            val j = contest(b, g, r)
            altersingle(alpha, j, b, g, r)
            if (rad != 0) alterneigh(rad, j, b, g, r)
            pix += step
            if (pix >= lim) pix -= lengthcount
            i++
            if (i % delta == 0) {
                alpha -= alpha / alphadec
                radius -= radius / RADIUSDEC
                rad = radius shr RADIUSBIASSHIFT
                if (rad <= 1) rad = 0
                for (jj in 0 until rad) radpower[jj] = alpha * (((rad * rad - jj * jj) * RADBIAS) / (rad * rad))
            }
        }
    }

    companion object {
        const val NETSIZE = 256
        const val PRIME1 = 499
        const val PRIME2 = 491
        const val PRIME3 = 487
        const val PRIME4 = 503
        const val MINPICTUREBYTES = 3 * PRIME4
        const val MAXNETPOS = NETSIZE - 1
        const val NETBIASSHIFT = 4
        const val NCYCLES = 100
        const val INTBIASSHIFT = 16
        const val INTBIAS = 1 shl INTBIASSHIFT
        const val GAMMASHIFT = 10
        const val BETASHIFT = 10
        const val BETA = INTBIAS shr BETASHIFT
        const val BETAGAMMA = INTBIAS shl (GAMMASHIFT - BETASHIFT)
        const val INITRAD = NETSIZE shr 3
        const val RADIUSBIASSHIFT = 6
        const val RADIUSBIAS = 1 shl RADIUSBIASSHIFT
        const val INITRADIUS = INITRAD * RADIUSBIAS
        const val RADIUSDEC = 30
        const val ALPHABIASSHIFT = 10
        const val INITALPHA = 1 shl ALPHABIASSHIFT
        const val RADBIASSHIFT = 8
        const val RADBIAS = 1 shl RADBIASSHIFT
        const val ALPHARADBSHIFT = ALPHABIASSHIFT + RADBIASSHIFT
        const val ALPHARADBIAS = 1 shl ALPHARADBSHIFT
    }
}

// ---------------------------------------------------------------------------
// GIF LZW encoder (public domain; from the classic GIF compress sources).
// ---------------------------------------------------------------------------
private class LZWEncoder(
    private val imgW: Int,
    private val imgH: Int,
    private val pixAry: ByteArray,
    colorDepth: Int
) {
    private val initCodeSize = maxOf(2, colorDepth)
    private var remaining = 0
    private var curPixel = 0

    private val maxbits = 12
    private val maxmaxcode = 1 shl maxbits
    private val htab = IntArray(HSIZE)
    private val codetab = IntArray(HSIZE)
    private var nBits = 0
    private var maxcode = 0
    private var freeEnt = 0
    private var clearFlg = false
    private var gInitBits = 0
    private var clearCode = 0
    private var eofCode = 0
    private var curAccum = 0
    private var curBits = 0
    private var aCount = 0
    private val accum = ByteArray(256)

    fun encode(os: OutputStream) {
        os.write(initCodeSize)
        remaining = imgW * imgH
        curPixel = 0
        compress(initCodeSize + 1, os)
        os.write(0) // block terminator
    }

    private fun maxCode(nBits: Int) = (1 shl nBits) - 1

    private fun compress(initBits: Int, outs: OutputStream) {
        gInitBits = initBits
        clearFlg = false
        nBits = gInitBits
        maxcode = maxCode(nBits)
        clearCode = 1 shl (initBits - 1)
        eofCode = clearCode + 1
        freeEnt = clearCode + 2
        aCount = 0
        var ent = nextPixel()
        var hshift = 0
        var fcode = HSIZE
        while (fcode < 65536) { hshift++; fcode *= 2 }
        hshift = 8 - hshift
        clHash(HSIZE)
        output(clearCode, outs)

        outer@ while (true) {
            val c = nextPixel()
            if (c == EOF) break
            fcode = (c shl maxbits) + ent
            var i = (c shl hshift) xor ent
            if (htab[i] == fcode) {
                ent = codetab[i]; continue
            } else if (htab[i] >= 0) {
                var disp = HSIZE - i
                if (i == 0) disp = 1
                do {
                    i -= disp
                    if (i < 0) i += HSIZE
                    if (htab[i] == fcode) { ent = codetab[i]; continue@outer }
                } while (htab[i] >= 0)
            }
            output(ent, outs)
            ent = c
            if (freeEnt < maxmaxcode) {
                codetab[i] = freeEnt++
                htab[i] = fcode
            } else {
                clBlock(outs)
            }
        }
        output(ent, outs)
        output(eofCode, outs)
    }

    private fun output(code: Int, outs: OutputStream) {
        curAccum = curAccum and MASKS[curBits]
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
                maxcode = maxCode(nBits)
                clearFlg = false
            } else {
                nBits++
                maxcode = if (nBits == maxbits) maxmaxcode else maxCode(nBits)
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

    private fun charOut(c: Byte, outs: OutputStream) {
        accum[aCount++] = c
        if (aCount >= 254) flushChar(outs)
    }

    private fun clBlock(outs: OutputStream) {
        clHash(HSIZE)
        freeEnt = clearCode + 2
        clearFlg = true
        output(clearCode, outs)
    }

    private fun clHash(hsize: Int) {
        for (i in 0 until hsize) htab[i] = -1
    }

    private fun flushChar(outs: OutputStream) {
        if (aCount > 0) {
            outs.write(aCount)
            outs.write(accum, 0, aCount)
            aCount = 0
        }
    }

    private fun nextPixel(): Int {
        if (remaining == 0) return EOF
        remaining--
        val pix = pixAry[curPixel++]
        return pix.toInt() and 0xff
    }

    companion object {
        const val EOF = -1
        const val HSIZE = 5003
        val MASKS = intArrayOf(
            0x0000, 0x0001, 0x0003, 0x0007, 0x000F, 0x001F, 0x003F, 0x007F,
            0x00FF, 0x01FF, 0x03FF, 0x07FF, 0x0FFF, 0x1FFF, 0x3FFF, 0x7FFF, 0xFFFF
        )
    }
}
