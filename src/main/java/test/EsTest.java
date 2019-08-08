package test;


import java.io.IOException;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import org.apache.http.HttpHost;
import org.apache.log4j.Logger;
import org.elasticsearch.action.ActionListener;
import org.elasticsearch.action.delete.DeleteRequest;
import org.elasticsearch.action.delete.DeleteResponse;
import org.elasticsearch.action.get.GetRequest;
import org.elasticsearch.action.get.GetResponse;
import org.elasticsearch.action.index.IndexRequest;
import org.elasticsearch.action.index.IndexResponse;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.client.RestClient;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.common.xcontent.XContentType;
import org.elasticsearch.search.fetch.subphase.FetchSourceContext;
import org.junit.Before;
import org.junit.Test;

import com.alibaba.fastjson.JSONObject;



/**
 * 
 * ���°汾���ǻ���7.1�汾������HighLevel
 */
public class EsTest {

	private static RestHighLevelClient client;
	
	private static final Logger logger = Logger.getLogger(EsTest.class);

	@Before
	public void initialize() {

		client = new RestHighLevelClient(RestClient.builder(new HttpHost("10.100.7.111", 9200, "http")));
	}
	
	/**
	 * ��String�ķ�ʽ��������
	 */
	@Test
	public void indexString() throws IOException {
		IndexRequest request = new IndexRequest("first_index");
		request.id("1");
		
		String jsonString = "{" +
		        "\"user\":\"frank\"," +
		        "\"postDate\":\"2018-08-08\"," +
		        "\"message\":\"trying out Elasticsearch\"" +
		        "}";
		request.source(jsonString, XContentType.JSON);
		IndexResponse indexResponse = client.index(request, RequestOptions.DEFAULT);
		System.out.println(JSONObject.toJSON(indexResponse));
	}
	/**
	 * ��Map����ʽ����,���ID��ͬ���Ǹ���
	 */
	@Test
	public void indexMap() throws IOException {
		
		Map<String, Object> jsonMap = new HashMap<>();
		jsonMap.put("user", "kimchy");
		jsonMap.put("postDate", new Date());
		jsonMap.put("message", "trying forth Elasticsearch");
		IndexRequest indexRequest = new IndexRequest("first_index")
		    .id("eagles").source(jsonMap).setIfSeqNo(5).setIfPrimaryTerm(1); 
		
		IndexResponse indexResponse = client.index(indexRequest, RequestOptions.DEFAULT);
		System.out.println(JSONObject.toJSON(indexResponse));
	}
	
	/**
	 * �첽�ķ�ʽ���и��� 
	 */
	@Test
	public void asynchronized() throws IOException, InterruptedException {
		
		Map<String, Object> jsonMap = new HashMap<>();
		jsonMap.put("user", "eagles");
		jsonMap.put("postDate", new Date());
		jsonMap.put("message", "trying sixth Elasticsearch");
		IndexRequest indexRequest = new IndexRequest("first_index")
		    .id("3").source(jsonMap).setIfSeqNo(5).setIfPrimaryTerm(1); 
		
		ActionListener<IndexResponse> actionListener = new ActionListener<IndexResponse>() {
		    @Override
		    public void onResponse(IndexResponse indexResponse) {
		    	System.out.println(JSONObject.toJSON(indexResponse));
		    	try {
					client.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
		    }
		    @Override
		    public void onFailure(Exception e) {
		    	logger.error("ERROR",e);
		    	System.out.println("����ʧ��");
		    }
		};
		client.indexAsync(indexRequest, RequestOptions.DEFAULT, actionListener);
		Thread.sleep(10000);
	}
	/**
	 * ��ѯ��ȡ 
	 */
	@Test
	public void getRequest() throws IOException {
		
		GetRequest request = new GetRequest( "first_index", "11");
		
		GetResponse getResponse = client.get(request, RequestOptions.DEFAULT);		
		System.out.println(JSONObject.toJSON(getResponse));
	}	
	/**
	 * �ж��Ƿ����,�����get��ֻ࣬��������
	 */
	@Test
	public void judgeExist() {
		
		GetRequest getRequest = new GetRequest(
			    "posts", 
			    "1");    
		getRequest.fetchSourceContext(new FetchSourceContext(false)); 
		getRequest.storedFields("_none_");   
	}
	
	
	/***
     * ɾ��API  
	 * @throws IOException 
	 */
	@Test
	public void delete() throws IOException {
		
		DeleteRequest request = new DeleteRequest(
		        "first_index",    
		        "4"); 
		DeleteResponse deleteResponse = client.delete(request, RequestOptions.DEFAULT);
		
		System.out.println(JSONObject.toJSON(deleteResponse));
	}
	
}