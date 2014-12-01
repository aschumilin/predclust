package util;

import java.io.BufferedOutputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

public class BufferedZipFileWriter {

	private String fileName;
	private FileOutputStream fos;
	private BufferedOutputStream bos ; // 2.000.000 = 20MB , 
	private ZipOutputStream zos ;
	
	public BufferedZipFileWriter(String filePath, double bufferSizeInMB, String unzippedItemName) throws IOException{
		fileName = filePath;
		int bufferSizeInBytes = new Double(bufferSizeInMB * 1000000).intValue();
		fos = new FileOutputStream(fileName);
		bos = new BufferedOutputStream(fos, bufferSizeInBytes);
		zos = new ZipOutputStream(bos);		
		zos.putNextEntry(new ZipEntry(unzippedItemName));		
	}
	
	public void close() throws IOException{
		zos.closeEntry();
		zos.close();
	}
	
	public void writeLine(String line) throws IOException{
		zos.write(line.getBytes());
		zos.write("\n".getBytes());
	}

}
