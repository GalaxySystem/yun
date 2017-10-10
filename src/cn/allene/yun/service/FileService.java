package cn.allene.yun.service;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.channels.FileChannel;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.io.IOUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import cn.allene.yun.dao.FileDao;
import cn.allene.yun.dao.OfficeDao;
import cn.allene.yun.dao.UserDao;
import cn.allene.yun.pojo.FileCustom;
import cn.allene.yun.pojo.RecycleFile;
import cn.allene.yun.pojo.User;
import cn.allene.yun.pojo.SummaryFile;
import cn.allene.yun.utils.FileUtils;
import cn.allene.yun.utils.UserUtils;

@Service
public class FileService {
	/**
	 * 文件相对前缀
	 */
	public static final String PREFIX = "WEB-INF" + File.separator + "file" + File.separator;
	/**
	 * 新用户注册默认文件夹
	 */
	public static final String[] DEFAULT_DIRECTORY = { "vido", "music", "source", "image", User.RECYCLE};
	@Autowired
	private UserDao userDao;
	@Autowired
	private FileDao fileDao;
	/**
	 * 上传文件
	 * @param request
	 * @param files			文件
	 * @param currentPath	当前路径
	 * @throws Exception
	 */
	public void uploadFilePath(HttpServletRequest request, MultipartFile[] files, String currentPath) throws Exception {
		for (MultipartFile file : files) {
			String fileName = file.getOriginalFilename();
			String filePath = getFileName(request, currentPath);
			File distFile = new File(filePath, fileName);
			if(!distFile.exists()){
				file.transferTo(distFile);
				if("office".equals(FileUtils.getFileType(distFile))){
					try {
						String suffix = fileName.substring(fileName.lastIndexOf(".") + 1);
						String documentId = FileUtils.getDocClient().createDocument(distFile, fileName, suffix).getDocumentId();
						officeDao.addOffice(documentId, FileUtils.MD5(distFile));
					} catch (Exception e) {
					}
				}
			}
		}
		reSize(request);
	}
	/**
	 * 删除压缩文件包
	 * @param downloadFile
	 */
	public void deleteDownPackage(File downloadFile) {
		if (downloadFile.getName().endsWith(".zip")) {
			downloadFile.delete();
		}
	}
	/**
	 * 下载文件打包
	 * @param request
	 * @param currentPath	当前路径
	 * @param fileNames		文件名
	 * @param username		用户名
	 * @return				打包的文件对象
	 * @throws Exception
	 */
	public File downPackage(HttpServletRequest request, String currentPath, String[] fileNames, String username)
			throws Exception {
		File downloadFile = null;
		if (currentPath == null) {
			currentPath = "";
		}
		if (fileNames.length == 1) {
			downloadFile = new File(getFileName(request, currentPath, username), fileNames[0]);
			if (downloadFile.isFile()) {
				return downloadFile;
			}
		}
		String[] sourcePath = new String[fileNames.length];
		for (int i = 0; i < fileNames.length; i++) {
			sourcePath[i] = getFileName(request, currentPath, username) + File.separator + fileNames[i];
		}
		String packageZipName = packageZip(sourcePath);
		downloadFile = new File(packageZipName);
		return downloadFile;
	}
	/**
	 * 压缩文件
	 * @param sourcePath
	 * @return
	 * @throws Exception
	 */
	private String packageZip(String[] sourcePath) throws Exception {
		String zipName = sourcePath[0] + (sourcePath.length == 1 ? "" : "等" + sourcePath.length + "个文件") + ".zip";
		ZipOutputStream zos = null;
		try {
			zos = new ZipOutputStream(new FileOutputStream(zipName));
			for (String string : sourcePath) {
				writeZos(new File(string), "", zos);
			}
		} finally {
			if(zos != null){
				zos.close();
			}
		}
		return zipName;
	}
	/**
	 * 写入文件到压缩包
	 * @param file
	 * @param basePath
	 * @param zos
	 * @throws IOException
	 */
	private void writeZos(File file, String basePath, ZipOutputStream zos) throws IOException {
		if (!file.exists()) {
			throw new FileNotFoundException();
		}
		if (file.isDirectory()) {
			File[] listFiles = file.listFiles();
			if (listFiles.length != 0) {
				for (File childFile : listFiles) {
					writeZos(childFile, basePath + file.getName() + File.separator, zos);
				}
			}
		} else {
			BufferedInputStream bis = new BufferedInputStream(new FileInputStream(file));
			ZipEntry entry = new ZipEntry(basePath + file.getName());
			zos.putNextEntry(entry);
			int count = 0;
			byte data[] = new byte[1024];
			while ((count = bis.read(data)) != -1) {
				zos.write(data, 0, count);
			}
			bis.close();
		}
	}
	/**
	 * 获取文件跟路径
	 * @param request
	 * @return
	 */
	public String getRootPath(HttpServletRequest request) {
		String rootPath = request.getSession().getServletContext().getRealPath("/") + PREFIX;
		return rootPath;
	}
	/**
	 * 获取回收站跟路径
	 * @param request
	 * @return
	 */
	public String getRecyclePath(HttpServletRequest request){
		return getFileName(request, User.RECYCLE);
	}
	/**
	 * 获取文件路径
	 * @param request
	 * @param fileName
	 * @return
	 */
	public String getFileName(HttpServletRequest request, String fileName) {
		if (fileName == null) {
			fileName = "";
		}
		String username = UserUtils.getUsername(request);
		return getRootPath(request) + username + File.separator + fileName;
	}
	/**
	 * 根据用户名获取文件路径
	 * @param request
	 * @param fileName
	 * @param username
	 * @return
	 */
	public String getFileName(HttpServletRequest request, String fileName, String username) {
		if (username == null) {
			return getFileName(request, fileName);
		}
		if (fileName == null) {
			fileName = "";
		}
		return getRootPath(request) + username + File.separator + fileName;
	}
	/**
	 * 获取路径下的文件类别
	 * @param realPath	路径
	 * @return
	 */
	public List<FileCustom> listFile(String realPath) {
		File[] files = new File(realPath).listFiles();
		List<FileCustom> lists = new ArrayList<FileCustom>();
		if (files != null) {
			for (File file : files) {
				if(!file.getName().equals(User.RECYCLE)){
					FileCustom custom = new FileCustom();
					custom.setFileName(file.getName());
					custom.setLastTime(FileUtils.formatTime(file.lastModified()));
					/*保存文件的删除前路径以及当前路径*/
//					custom.setFilePath(prePath);
					custom.setCurrentPath(realPath);
					if (file.isDirectory()) {
						custom.setFileSize("-");
					} else {
						custom.setFileSize(FileUtils.getDataSize(file.length()));
					}
					custom.setFileType(FileUtils.getFileType(file));
					lists.add(custom);
				}
			}
		}
		return lists;
	}
//
//	public List<FileCustom> searchFile(HttpServletRequest request, String currentPath, String reg) {
//		List<FileCustom> list = new ArrayList<>();
//		matchFile(list, new File(getFileName(request, currentPath)), reg, "");
//		return list;
//	}
	/**
	 * 查找文件
	 * @param request
	 * @param currentPath	当前路径
	 * @param reg			文件名字
	 * @param regType		文件类型
	 * @return
	 */
	public List<FileCustom> searchFile(HttpServletRequest request, String currentPath, String reg, String regType) {
		List<FileCustom> list = new ArrayList<>();
		matchFile(request, list, new File(getFileName(request, currentPath)), reg, regType == null ? "" : regType);
		return list;
	}
	private void matchFile(HttpServletRequest request, List<FileCustom> list, File dirFile, String reg, String regType) {
		File[] listFiles = dirFile.listFiles();
		if (listFiles != null) {
			for (File file : listFiles) {
				if (file.isFile()) {
					String suffixType = FileUtils.getFileType(file);
					if (suffixType.equals(regType) || (reg != null && file.getName().contains(reg))) {
						FileCustom custom = new FileCustom();
						custom.setFileName(file.getName());
						custom.setLastTime(FileUtils.formatTime(file.lastModified()));
						String parentPath = file.getParent();
						String prePath = parentPath.substring(parentPath.indexOf(getFileName(request, null)) + getFileName(request, null).length());
						custom.setCurrentPath(File.separator + prePath);
						if (file.isDirectory()) {
							custom.setFileSize("-");
						} else {
							custom.setFileSize(FileUtils.getDataSize(file.length()));
						}
						custom.setFileType(FileUtils.getFileType(file));
						list.add(custom);
					}
				} else {
					matchFile(request, list, file, reg, regType);
				}
			}
		}
	}
	/**
	 * 移动的文件列表
	 * @param realPath	路径
	 * @param number	该路径下的文件数量
	 * @return
	 */
	public SummaryFile summarylistFile(String realPath, int number) {
		File file = new File(realPath);
		SummaryFile sF = new SummaryFile();
		List<SummaryFile> returnlist = new ArrayList<SummaryFile>();
		if (file.isDirectory()) {
			sF.setisFile(false);
			if (realPath.length() <= number) {
				sF.setfileName("yun盘");
				sF.setPath("");
			} else {
				String path = file.getPath();
				sF.setfileName(file.getName());
				sF.setPath(path.substring(number));
			}
			/* 设置抽象文件夹的包含文件集合 */
			for (File filex : file.listFiles()) {
				SummaryFile innersF = summarylistFile(filex.getPath(), number);
				if (!innersF.getisFile()) {
					returnlist.add(innersF);
				}
			}
			sF.setListFile(returnlist);
			/* 设置抽象文件夹的包含文件夹个数 */
			sF.setListdiretory(returnlist.size());

		} else {
			sF.setisFile(true);
		}
		return sF;
	}
	/**
	 * 新建文件夹
	 * @param request
	 * @param currentPath	当前路径
	 * @param directoryName	文件夹名
	 * @return
	 */
	public boolean addDirectory(HttpServletRequest request, String currentPath, String directoryName) {
		File file = new File(getFileName(request, currentPath), directoryName);
		return file.mkdir();
	}
	
	
	/*--回收站显示所有删除文件--*/
	public List<RecycleFile> recycleFiles(HttpServletRequest request) throws Exception{
		List<RecycleFile> recycleFiles = fileDao.selectFiles(UserUtils.getUsername(request));
		for(RecycleFile file : recycleFiles){
			File f = new File(getRecyclePath(request), new File(file.getFilePath()).getName());
			file.setFileName(f.getName());
			file.setLastTime(FileUtils.formatTime(f.lastModified()));
		}
		return recycleFiles;
	}
	
	/*删除回收站的文件*/
	public void delRecycle(HttpServletRequest request,int[] fileId) throws Exception{	
		for (int i = 0;i < fileId.length;i++) {
			RecycleFile selectFile = fileDao.selectFile(fileId[i]);
			File srcFile = new File(getRecyclePath(request), selectFile.getFilePath());
			fileDao.deleteFile(fileId[i], UserUtils.getUsername(request));
			delFile(srcFile);
		}
		reSize(request);
	}
	
	/*--依次遍历recycle下各个文件，并删除--*/
	public void delAllRecycle(HttpServletRequest request) throws Exception{
		File file = new File(getRecyclePath(request));
		File[] inferiorFile = file.listFiles();
		for(File f : inferiorFile){
			delFile(f);
		}
		fileDao.deleteFiles(UserUtils.getUsername(request));
		reSize(request);
	}
	/**
	 * 删除文件
	 * @param request
	 * @param currentPath	当前路径
	 * @param directoryName	文件名
	 * @throws Exception
	 */
	public void delDirectory(HttpServletRequest request, String currentPath, String[] directoryName) throws Exception {
		for (String fileName : directoryName) {
			String srcPath = currentPath + File.separator + fileName;
			File src = new File(getFileName(request, srcPath));
			File dest = new File(getRecyclePath(request));
			org.apache.commons.io.FileUtils.moveToDirectory(src, dest, true);
			fileDao.insertFiles(srcPath, UserUtils.getUsername(request));
			/*--将删除文件移动到recycle目录下*/
//			moveDirectory(request,currentPath,directoryName,User.RECYCLE);			
		}
		/*--获取文件删除前的路径--*/
		reSize(request);
	}
	/*还原文件*/
	public void revertDirectory(HttpServletRequest request,int[] fileId) throws Exception{
		for(int id : fileId){
			RecycleFile file = fileDao.selectFile(id);
			String fileName = new File(file.getFilePath()).getName();
			File src = new File(getRecyclePath(request), fileName);
			File dest = new File(getFileName(request, file.getFilePath()));
			org.apache.commons.io.FileUtils.moveToDirectory(src, dest.getParentFile(), true);
			fileDao.deleteFile(id, UserUtils.getUsername(request));
		}
	}
	/**
	 * 删除文件
	 * @param srcFile	源文件
	 * @throws Exception
	 */
	private void delFile(File srcFile) throws Exception {
		/* 如果是文件，直接删除 */
		
		if (!srcFile.isDirectory()) {
			/* 使用map 存储删除的 文件路径，同时保存用户名*/
			srcFile.delete();
			return;
		}
		/* 如果是文件夹，再遍历 */
		File[] listFiles = srcFile.listFiles();
		for (File file : listFiles) {
			if (file.isDirectory()) {
				delFile(file);
			} else {
				if (file.exists()) {
					file.delete();
				}
			}
		}
		if (srcFile.exists()) {
			srcFile.delete();
		}
	}
	/**
	 * 重命名文件
	 * @param request
	 * @param currentPath
	 * @param srcName
	 * @param destName
	 * @return
	 */
	public boolean renameDirectory(HttpServletRequest request, String currentPath, String srcName, String destName) {
		File file = new File(getFileName(request, currentPath), srcName);
		File descFile = new File(getFileName(request, currentPath), destName);
		return file.renameTo(descFile);
	}
	/**
	 * 新用户新建文件
	 * @param request
	 * @param namespace
	 */
	public void addNewNameSpace(HttpServletRequest request, String namespace) {
		String fileName = getRootPath(request);
		File file = new File(fileName, namespace);
		file.mkdir();
		for (String newFileName : DEFAULT_DIRECTORY) {
			File newFile = new File(file, newFileName);
			newFile.mkdir();
		}
	}
	/**
	 * copy文件
	 * @param srcFile	源文件
	 * @param targetFile目标文件
	 * @throws IOException
	 */
	private void copyfile(File srcFile, File targetFile) throws IOException {
		// TODO Auto-generated method stub
		if (!srcFile.isDirectory()) {
			/* 如果是文件，直接复制 */
			targetFile.createNewFile();
			FileInputStream src = (new FileInputStream(srcFile));
			FileOutputStream target = new FileOutputStream(targetFile);
			FileChannel in = src.getChannel();
			FileChannel out = target.getChannel();
			in.transferTo(0, in.size(), out);
			src.close();
			target.close();
		} else {
			/* 如果是文件夹，再遍历 */
			File[] listFiles = srcFile.listFiles();
			targetFile.mkdir();
			for (File file : listFiles) {
				File realtargetFile = new File(targetFile, file.getName());
				copyfile(file, realtargetFile);
			}
		}
	}
	/**
	 * 移动文件
	 * @param request
	 * @param currentPath			当前路径
	 * @param directoryName			文件名
	 * @param targetdirectorypath	目标路径
	 * @throws Exception
	 */
	public void moveDirectory(HttpServletRequest request, String currentPath, String[] directoryName,
			String targetdirectorypath) throws Exception {
		// TODO Auto-generated method stub
		for (String srcName : directoryName) {
			File srcFile = new File(getFileName(request, currentPath), srcName);
			File targetFile = new File(getFileName(request, targetdirectorypath), srcName);
			/* 处理目标目录中存在同名文件或文件夹问题 */
			if (srcFile.isDirectory()) {
				if (targetFile.exists()) {
					for (int i = 1; !targetFile.mkdir(); i++) {
						targetFile = new File(targetFile.getParentFile(), srcName + "(" + i + ")");
					}
					;
				}
			} else {
				if (targetFile.exists()) {
					for (int i = 1; !targetFile.createNewFile(); i++) {
						targetFile = new File(targetFile.getParentFile(), srcName + "(" + i + ")");
					}
				}
			}

			/* 移动即先复制，再删除 */
			copyfile(srcFile, targetFile);
			delFile(srcFile);
		}
	}
	/**
	 * 递归统计用户文件大小
	 * @param srcFile	位置
	 * @return
	 */
	private long countFileSize(File srcFile) {
		File[] listFiles = srcFile.listFiles();
		if (listFiles == null) {
			return 0;
		}
		long count = 0;
		for (File file : listFiles) {
			if (file.isDirectory()) {
				count += countFileSize(file);
			} else {
				count += file.length();
			}
		}
		return count;
	}
	/**
	 * 统计用户文件大小
	 * @param request
	 * @return
	 */
	public String countFileSize(HttpServletRequest request) {
		long countFileSize = countFileSize(new File(getFileName(request, null)));
		return FileUtils.getDataSize(countFileSize);
	}
	/**
	 * 重新计算文件大小
	 * @param request
	 */
	public void reSize(HttpServletRequest request) {
		String userName = UserUtils.getUsername(request);
		try {
			userDao.reSize(userName, countFileSize(request));
		} catch (Exception e) {
			e.printStackTrace();

		}
	}
	@Autowired
	private OfficeDao officeDao;
	/**
	 * 响应文件流 
	 * @param response
	 * @param request
	 * @param currentPath	当前路径
	 * @param fileName		文件名
	 * @param type			文件类型
	 * @throws IOException
	 */
	public void respFile(HttpServletResponse response, HttpServletRequest request, String currentPath, String fileName, String type) throws IOException {
		File file = new File(getFileName(request, currentPath), fileName);
		InputStream inputStream = new FileInputStream(file);
		if("docum".equals(type)){
			response.setCharacterEncoding("UTF-8");
			IOUtils.copy(inputStream, response.getWriter(), "UTF-8");
		}else{
			IOUtils.copy(inputStream, response.getOutputStream());
		}
	}
	/**
	 * 打开文档文件
	 * @param request
	 * @param currentPath
	 * @param fileName
	 * @return
	 * @throws Exception
	 */
	public String openOffice(HttpServletRequest request, String currentPath, String fileName) throws Exception {
		 return officeDao.getOfficeId(FileUtils.MD5(new File(getFileName(request, currentPath), fileName)));
	}
}
