package org.opencarto.util;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.net.SocketException;

import org.apache.commons.net.PrintCommandListener;
import org.apache.commons.net.ftp.FTP;
import org.apache.commons.net.ftp.FTPClient;
import org.apache.commons.net.ftp.FTPReply;

public class FTPConnection {
	private static final String sep = System.getProperties().getProperty("file.separator");

	private FTPClient ftp = null;
	private String host,user,pwd;
	private int bufferSize = -1,fileType = FTP.ASCII_FILE_TYPE;
	private boolean showFTPMessages = true;

	public FTPConnection(String host, String user, String pwd, boolean showFTPMessages) { this(host, user, pwd, -1, FTP.ASCII_FILE_TYPE, showFTPMessages); }
	public FTPConnection(String host, String user, String pwd, int bufferSize, int fileType, boolean showFTPMessages) {
		this.host = host;
		this.user = user;
		this.pwd = pwd;
		this.bufferSize = bufferSize;
		this.fileType = fileType;
		this.showFTPMessages = showFTPMessages;
		ftp = new FTPClient();
		connect();
	}

	public FTPConnection connect() {
		if(ftp.isConnected()) return this;
		try {
			ftp.connect(host);
			int reply = ftp.getReplyCode();
			if (!FTPReply.isPositiveCompletion(reply)) {
				ftp.disconnect();
				System.out.println("Problem in connecting to FTP Server");
			}
			System.out.println("Login: "+ftp.login(user, pwd));
			System.out.println("Set file type: "+ftp.setFileType(fileType));
			ftp.enterLocalPassiveMode();
			if(bufferSize>0) ftp.setBufferSize(bufferSize);
			if(showFTPMessages) ftp.addProtocolCommandListener(new PrintCommandListener(new PrintWriter(System.out)));
			System.out.println("Connection OK - " + reply);
		}
		catch (SocketException e) { e.printStackTrace(); }
		catch (IOException e) { e.printStackTrace(); }
		return this;
	}

	public FTPConnection disconnect(){
		if (!ftp.isConnected()) return this;
		try {
			ftp.logout();
			ftp.disconnect();
		} catch (Exception f) {}
		return this;
	}


	//make directory on ftp server
	public FTPConnection makeDirectory(String outPath){
		try {
			ftp.makeDirectory(outPath);
		} catch (Exception e) {
			//if ftp has been disconnected, reconnect and retry
			if(!ftp.isConnected()) {
				connect();
				makeDirectory(outPath);
				return this;
			}
			e.printStackTrace();
		}
		return this;
	}

	//upload a single file
	private FTPConnection uploadFile(String inFilePath, String outFilePath){
		//java.net.ConnectException
		//org.apache.commons.net.ftp.FTPConnectionClosedException: FTP response 421 received.  Server closed connection.
		try {
			FileInputStream is = new FileInputStream(new File(inFilePath));
			ftp.storeFile(outFilePath, is);
			is.close();
		} catch (Exception e) {
			//if ftp has been disconnected, reconnect and retry
			if(!ftp.isConnected()) {
				connect();
				uploadFile(inFilePath, outFilePath);
				return this;
			}
			e.printStackTrace();
		}
		return this;
	}

	//upload a file or directory (recursivelly) to a remote folder
	public FTPConnection upload(String inPath, String outFolderPath) {
		File inPath_ = new File(inPath);
		if(!inPath_.exists()){
			System.out.println("Source " + inPath + " does not exist.");
			return this;
		}
		if(!inPath_.isDirectory()){
			//upload file
			System.out.println("Upload "+inPath_+" to "+outFolderPath);
			uploadFile(inPath, outFolderPath + sep + inPath_.getName());
		} else {
			//recursive call
			String elts[] = inPath_.list();
			for (String elt : elts) {
				if(!new File(inPath + sep + elt).isDirectory()){
					upload(inPath + sep + elt, outFolderPath);
				} else {
					System.out.println("Upload directory " + outFolderPath + sep + elt);
					makeDirectory(outFolderPath + sep + elt);
					upload(inPath + sep + elt, outFolderPath + sep + elt);
				}
			}
		}
		return this;
	}

}
