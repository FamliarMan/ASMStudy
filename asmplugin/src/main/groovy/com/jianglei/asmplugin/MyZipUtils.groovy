package com.jianglei.asmplugin

import java.util.zip.CRC32
import java.util.zip.CheckedOutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipFile
import java.util.zip.ZipOutputStream

class MyZipUtils {

    public static void zip(String srcPath, String dstPath) throws IOException {
        File srcFile = new File(srcPath)
        File dstFile = new File(dstPath)
        if (!srcFile.exists()) {
            throw new FileNotFoundException(srcPath + "不存在！")
        }

        FileOutputStream out = null
        ZipOutputStream zipOut = null
        try {
            out = new FileOutputStream(dstFile)
            CheckedOutputStream cos = new CheckedOutputStream(out, new CRC32())
            zipOut = new ZipOutputStream(cos)
            String baseDir = ""
            zipFileOrDirectory(srcFile, zipOut, baseDir)
        }
        finally {
            if (null != zipOut) {
                zipOut.close()
                out = null
            }

            if (null != out) {
                out.close()
            }
        }
    }

    private static void zipFileOrDirectory(File file, ZipOutputStream zipOut, String baseDir) throws IOException {
        if (file.isDirectory()) {
            compressDirectory(file, zipOut, baseDir)
        } else {
            compressFile(file, zipOut, baseDir)
        }
    }

/** 压缩一个目录 */
    private static void compressDirectory(File dir, ZipOutputStream zipOut, String baseDir) throws IOException {
        File[] files = dir.listFiles()
        for (int i = 0; i < files.length; i++) {
            zipFileOrDirectory(files[i], zipOut, baseDir + dir.getName() + "/")
        }
    }

/** 压缩一个文件 */
    private static void compressFile(File file, ZipOutputStream zipOut, String baseDir) throws IOException {
        if (!file.exists()) {
            return
        }

        BufferedInputStream bis = null
        try {
            bis = new BufferedInputStream(new FileInputStream(file))
            ZipEntry entry = new ZipEntry(baseDir + file.getName())
            zipOut.putNextEntry(entry)
            int count
            def data = new byte[2048]
            while ((count = bis.read(data, 0, 2048)) != -1) {
                zipOut.write(data, 0, count)
            }

        } finally {
            if (null != bis) {
                bis.close()
            }
        }
    }

    public static void unzip(String zipFile, String dstPath) throws IOException {
        File pathFile = new File(dstPath)
        if (!pathFile.exists()) {
            pathFile.mkdirs()
        }
        def zip = new ZipFile(zipFile)
        for (Enumeration entries = zip.entries(); entries.hasMoreElements();) {
            ZipEntry entry = (ZipEntry) entries.nextElement()
            String zipEntryName = entry.getName()
            InputStream ins = null
            OutputStream out = null
            try {
                ins = zip.getInputStream(entry)
                String outPath = (dstPath + File.separator + zipEntryName)
                //判断路径是否存在,不存在则创建文件路径
                File file = new File(outPath.substring(0, outPath.lastIndexOf(File.separator)))
                if (!file.exists()) {
                    file.mkdirs()
                }
                //判断文件全路径是否为文件夹,如果是上面已经上传,不需要解压
                if (new File(outPath).isDirectory()) {
                    continue
                }

                out = new FileOutputStream(outPath)
                byte[] buf1 = new byte[2048]
                int len
                while ((len = ins.read(buf1)) > 0) {
                    out.write(buf1, 0, len)
                }
            }
            finally {
                if (null != ins) {
                    ins.close()
                }

                if (null != out) {
                    out.close()
                }
            }
        }
    }
}