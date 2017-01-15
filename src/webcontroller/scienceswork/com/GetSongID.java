package webcontroller.scienceswork.com;

import java.io.ByteArrayOutputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.Charset;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.csvreader.CsvWriter;

import cn.edu.hfut.dmic.webcollector.model.CrawlDatums;
import cn.edu.hfut.dmic.webcollector.model.Page;
import cn.edu.hfut.dmic.webcollector.plugin.berkeley.BreadthCrawler;
import org.json.*;  

/**
 * 获取网易云音乐所有歌曲的id，并且写入csv文件中
 * @author scienceswork
 */
public class GetSongID extends BreadthCrawler {
	// 添加变量为CsvWriter
	private CsvWriter r = null;
	/**
	 * 关闭csv文件
	 */
	public void closeCsv() {
		this.r.close();
	}
	/**
	 * 将二进制流转化为byte字节数组
	 * @param instream
	 * @return
	 * @throws IOException
	 */
	public static byte[] readInputStream(InputStream instream) throws IOException {
        ByteArrayOutputStream outStream = new ByteArrayOutputStream();  
        byte[]  buffer = new byte[1204];  
        int len = 0;  
        while ((len = instream.read(buffer)) != -1){  
            outStream.write(buffer,0,len);  
        }  
        instream.close();  
        return outStream.toByteArray(); 
	}
	/**
	 * 根据URL获得网页源码
	 * @param url 传入的URL
	 * @return String
	 * @throws IOException 
	 */
	public static String getURLSource(URL url) throws IOException {
		HttpURLConnection conn = (HttpURLConnection)url.openConnection();
		conn.setRequestMethod("GET");
		conn.setConnectTimeout(5 * 1000);
		InputStream inStream = conn.getInputStream();
		byte[] data = readInputStream(inStream);
		String htmlSource = new String(data);
		return htmlSource;
	}
	/**
	 * 重写构造函数
	 * @param crawlPath 爬虫路径
	 * @param autoParse 是否自动解析
	 */
	public GetSongID(String crawlPath, boolean autoParse) throws FileNotFoundException {
		super(crawlPath, autoParse);
		// 逗号进行分割，字符编码为GBK
		this.r = new CsvWriter("songId.csv", ',', Charset.forName("GBK"));		
	}
	@Override
	public void visit(Page page, CrawlDatums next) {
		// 继承覆盖visit方法，该方法表示在每个页面进行的操作
		// 参数page和next分别表示当前页面和下个URL对象的地址
		// 生成文件songId.csv，第一列为歌曲id，第二列为歌曲名字，第三列为演唱者，第四列为歌曲信息的URL
		// 网易云音乐song页面URL地址正则
		String song_regex = "^http://music.163.com/song\\?id=[0-9]+";
		// 创建Pattern对象
		Pattern songIdPattern = Pattern.compile("^http://music.163.com/song\\?id=([0-9]+)");
		Pattern songInfoPattern = Pattern.compile("(.*?)-(.*?)-");
		// 对页面进行正则判断，如果有的话，将歌曲的id和网页标题提取出来，否则不进行任何操作
		if (Pattern.matches(song_regex, page.getUrl())) {
			// 将网页的URL和网页标题提取出来，网页标题格式：歌曲名字-歌手-网易云音乐
			String url = page.getUrl();
			@SuppressWarnings("deprecation")
			String title = page.getDoc().title();
			String songName = null;
			String songSinger = null;
			String songId = null;
			String infoUrl = null;
			String mp3Url = null;
			// 对标题进行歌曲名字、歌手解析
			Matcher infoMatcher = songInfoPattern.matcher(title);
			if (infoMatcher.find()) {
				songName = infoMatcher.group(1);
				songSinger = infoMatcher.group(2); 
			}
			System.out.println("正在抽取:" + url);
			// 创建Matcher对象，使用正则找出歌曲对应id
			Matcher idMatcher = songIdPattern.matcher(url);
			if (idMatcher.find()) {
				songId = idMatcher.group(1);
			}
			System.out.println("歌曲:" + songName);
			System.out.println("演唱者:" + songSinger);
			System.out.println("ID:" + songId);	
			infoUrl = "http://music.163.com/api/song/detail/?id=" + songId + "&ids=%5B+" + songId + "%5D";
			try {
				URL urlObject = new URL(infoUrl);
				// 获取json源码
				String urlsource = getURLSource(urlObject);
				JSONObject j = new JSONObject(urlsource);
				JSONArray a = (JSONArray) j.get("songs");
				JSONObject aa = (JSONObject) a.get(0);
				mp3Url = aa.get("mp3Url").toString();				
			} catch (Exception e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			String[] contents = {songId, songName, songSinger, url, mp3Url};
			try {
				this.r.writeRecord(contents);
				this.r.flush();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
	}
	/**
	 * 歌曲id爬虫开始
	 * @param args
	 * @throws Exception 
	 */
	public static void main(String[] args) throws Exception {
//		URL url = new URL("http://music.163.com/api/song/detail/?id=414979184&ids=%5B414979184%5D");
//		String urlsource = getURLSource(url);
//		System.out.println(urlsource);
//		JSONObject j = new JSONObject(urlsource);
//		JSONArray a = (JSONArray) j.get("songs");
//		JSONObject aa = (JSONObject) a.get(0);
//		System.out.println(aa.get("mp3Url"));
		GetSongID crawler = new GetSongID("crawler", true);
		// 添加初始种子页面http://music.163.com
		crawler.addSeed("http://music.163.com/#/album?id=2884361");
		// 设置采集规则为所有类型的网页
		crawler.addRegex("http://music.163.com/.*");
		// 设置爬取URL数量的上限
		crawler.setTopN(500000000);
		// 设置线程数
		crawler.setThreads(30);
		// 设置断点采集
		crawler.setResumable(false);
		// 设置爬虫深度
		crawler.start(5);
	}
}
