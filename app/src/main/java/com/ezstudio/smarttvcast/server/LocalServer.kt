package com.ezstudio.smarttvcast.server

import com.ezstudio.smarttvcast.utils.FileUtils
import fi.iki.elonen.NanoHTTPD
import fi.iki.elonen.util.ServerRunner
import java.io.*
import java.nio.charset.Charset


class LocalServer(var localFilePath: String, var style: String) : NanoHTTPD(8080) {
    fun main(args: Array<String?>?) {
        ServerRunner.run(LocalServer::class.java)
    }

//    override fun serve(
//        uri: String,
//        method: Method,
//        header: Map<String?, String?>?,
//        parameters: Map<String?, String?>?,
//        files: Map<String?, String?>?
//    ): Response {
//        var fis: FileInputStream? = null
//        val file = File(localFilePath)
//        try {
//            fis = FileInputStream(file)
//        } catch (e: FileNotFoundException) {
//            e.printStackTrace()
//        }
//        val res = newChunkedResponse(
//            Response.Status.OK,
//            "$style/${FileUtils.getFileExtension(localFilePath)}",
//            fis
//        )
//        res.addHeader("Accept-Ranges", "bytes")
//        return res
//    }

    override fun serve(
        uri: String, method: Method?,
        header: Map<String, String>, parameters: Map<String?, String?>?,
        files: Map<String?, String?>?
    ): Response {
        val f = File(localFilePath)
        val mimeType = "$style/${FileUtils.getFileExtension(localFilePath)}"
        return serveFile(uri, header, f, mimeType)
    }

    //Announce that the file server accepts partial content requests
    private fun createResponse(
        status: Response.Status, mimeType: String,
        message: InputStream
    ): Response {
        val res = newChunkedResponse(status, mimeType, message)
        res.addHeader("Accept-Ranges", "bytes")
        return res
    }

    /**
     * Serves file from homeDir and its' subdirectories (only). Uses only URI,
     * ignores all headers and HTTP parameters.
     */
    private fun serveFile(
        uri: String, header: Map<String, String>,
        file: File, mime: String
    ): Response {
        var res: Response
        try {
            // Calculate etag
            val etag = Integer.toHexString(
                (file.absolutePath
                        + file.lastModified() + "" + file.length()).hashCode()
            )

            // Support (simple) skipping:
            var startFrom: Long = 0
            var endAt: Long = -1
            var range = header["range"]
            if (range != null) {
                if (range.startsWith("bytes=")) {
                    range = range.substring("bytes=".length)
                    val minus = range.indexOf('-')
                    try {
                        if (minus > 0) {
                            startFrom = range
                                .substring(0, minus).toLong()
                            endAt = range.substring(minus + 1).toLong()
                        }
                    } catch (ignored: NumberFormatException) {
                    }
                }
            }

            // Change return code and add Content-Range header when skipping is
            // requested
            val fileLen = file.length()
            if (range != null && startFrom >= 0) {
                if (startFrom >= fileLen) {
                    res = createResponse(
                        Response.Status.RANGE_NOT_SATISFIABLE,
                        MIME_PLAINTEXT,
                        ByteArrayInputStream(Charset.forName("UTF-16").encode("").array())
                    )
                    res.addHeader("Content-Range", "bytes 0-0/$fileLen")
                    res.addHeader("ETag", etag)
                } else {
                    if (endAt < 0) {
                        endAt = fileLen - 1
                    }
                    var newLen = endAt - startFrom + 1
                    if (newLen < 0) {
                        newLen = 0
                    }
                    val dataLen = newLen
                    val fis: FileInputStream = object : FileInputStream(file) {
                        @Throws(IOException::class)
                        override fun available(): Int {
                            return dataLen.toInt()
                        }
                    }
                    fis.skip(startFrom)
                    res = createResponse(
                        Response.Status.PARTIAL_CONTENT, mime,
                        fis
                    )
                    res.addHeader("Content-Length", "" + dataLen)
                    res.addHeader(
                        "Content-Range", "bytes " + startFrom + "-"
                                + endAt + "/" + fileLen
                    )
                    res.addHeader("ETag", etag)
                }
            } else {
                if (etag == header["if-none-match"]) res =
                    createResponse(
                        Response.Status.NOT_MODIFIED, mime, ByteArrayInputStream(
                            Charset.forName("UTF-16").encode("").array()
                        )
                    ) else {
                    res = createResponse(
                        Response.Status.OK, mime,
                        FileInputStream(file)
                    )
                    res.addHeader("Content-Length", "" + fileLen)
                    res.addHeader("ETag", etag)
                }
            }
        } catch (ioe: IOException) {
            res = createResponse(
                Response.Status.FORBIDDEN,
                MIME_PLAINTEXT,
                ByteArrayInputStream(
                    Charset.forName("UTF-16").encode("FORBIDDEN: Reading file failed.").array()
                )
            )
        }
        return res
    }

}