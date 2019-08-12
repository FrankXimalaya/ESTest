package test;


import java.io.IOException;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import org.apache.http.HttpHost;
import org.apache.log4j.Logger;
import org.elasticsearch.action.ActionListener;
import org.elasticsearch.action.DocWriteResponse;
import org.elasticsearch.action.bulk.BulkItemResponse;
import org.elasticsearch.action.bulk.BulkRequest;
import org.elasticsearch.action.bulk.BulkResponse;
import org.elasticsearch.action.delete.DeleteRequest;
import org.elasticsearch.action.delete.DeleteResponse;
import org.elasticsearch.action.get.GetRequest;
import org.elasticsearch.action.get.GetResponse;
import org.elasticsearch.action.get.MultiGetRequest;
import org.elasticsearch.action.index.IndexRequest;
import org.elasticsearch.action.index.IndexResponse;
import org.elasticsearch.action.search.SearchRequest;
import org.elasticsearch.action.update.UpdateRequest;
import org.elasticsearch.action.update.UpdateResponse;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.client.RestClient;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.client.core.CountRequest;
import org.elasticsearch.client.core.CountResponse;
import org.elasticsearch.common.unit.TimeValue;
import org.elasticsearch.common.xcontent.XContentType;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.index.query.RangeQueryBuilder;
import org.elasticsearch.search.builder.SearchSourceBuilder;
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
	
	
	/***
      * ����  
	 * @throws IOException 
	 */
	@Test
	public void update() throws IOException {
		
		Map<String, Object> map = new HashMap<String, Object>();
		map.put("age", 100000);
		UpdateRequest request = new UpdateRequest("first_index","6").doc(map);
		client.update(request, RequestOptions.DEFAULT);
		
	}
	
	@Test
	public void bulkIndex() throws IOException {
		
		BulkRequest request = new BulkRequest(); 
		request.add(new IndexRequest("first_index").id("1")  
		        .source(XContentType.JSON,"field", "foo"));
		request.add(new IndexRequest("first_index").id("2")  
		        .source(XContentType.JSON,"field", "bar"));
		request.add(new IndexRequest("first_index").id("3")  
		        .source(XContentType.JSON,"field", "baz"));
        
		BulkResponse bulkResponse = client.bulk(request, RequestOptions.DEFAULT);
		
		//����ط���װͦţ�Ƶģ�ʵ����iteratable�ӿڣ���ֱ�ӿ�����forѭ����
		for (BulkItemResponse bulkItemResponse : bulkResponse) { 
		    DocWriteResponse itemResponse = bulkItemResponse.getResponse(); 

		    switch (bulkItemResponse.getOpType()) {
		    case INDEX:    
		    case CREATE:
		        IndexResponse indexResponse = (IndexResponse) itemResponse;
		        break;
		    case UPDATE:   
		        UpdateResponse updateResponse = (UpdateResponse) itemResponse;
		        break;
		    case DELETE:   
		        DeleteResponse deleteResponse = (DeleteResponse) itemResponse;
		    }
		}
	}
	
	@Test
	public void bulkGet() throws IOException {
		
		MultiGetRequest request = new MultiGetRequest();
		request.add(new MultiGetRequest.Item(
		    "first_index",         
		    "1"));  
		request.add(new MultiGetRequest.Item("first_index", "2")); 
		
		System.out.println(JSONObject.toJSON(client.multiGet(request, RequestOptions.DEFAULT)));
	}
	
	
	@Test
	public void searchBasic() throws IOException {
		
		SearchSourceBuilder searchsourceBuilder = new SearchSourceBuilder();
		searchsourceBuilder.query(QueryBuilders.matchAllQuery());
		
		
		SearchRequest request = new SearchRequest("first_index");
		request.source(searchsourceBuilder);
		
		
		System.out.println(JSONObject.toJSON(client.search(request, RequestOptions.DEFAULT)));

	}
	
	@Test
	public void searchCondition() throws IOException {
		
		SearchSourceBuilder sourceBuilder = new SearchSourceBuilder(); 
		sourceBuilder.query(QueryBuilders.termQuery("money", "1000")); 
		sourceBuilder.from(0); 
		sourceBuilder.size(5); 
		sourceBuilder.timeout(new TimeValue(60, TimeUnit.SECONDS)); 
		
		SearchRequest request = new SearchRequest("first_index");
		request.source(sourceBuilder);
		
		System.out.println(JSONObject.toJSON(client.search(request, RequestOptions.DEFAULT)));

	}
	
	@Test
	public void searchConbination() throws IOException {
		SearchSourceBuilder sourceBuilder = new SearchSourceBuilder();
		sourceBuilder.query(QueryBuilders.boolQuery().filter(QueryBuilders.termQuery("age", 34))).from(0).size(3);
		
		SearchRequest request = new SearchRequest("accesslog");
		request.source(sourceBuilder);
		
		System.out.println(JSONObject.toJSON(client.search(request, RequestOptions.DEFAULT)));
	}
	
	@Test
	public void countAPI() throws IOException {
		
		CountRequest request = new CountRequest("accesslog");
		SearchSourceBuilder searchsourceBuider = new SearchSourceBuilder();
		searchsourceBuider.query(QueryBuilders.matchAllQuery());
		
		CountResponse countResponse = client.count(request, RequestOptions.DEFAULT);
		System.out.println("--------- response----"+countResponse.getCount());
	}
	
	@Test
	public void countConditionAPI() throws IOException {
		
		CountRequest request = new CountRequest("accesslog");
		SearchSourceBuilder searchsourceBuider = new SearchSourceBuilder();
		searchsourceBuider.query(QueryBuilders.termQuery("face.age", 64));
		
		request.source(searchsourceBuider);
		
		CountResponse countResponse = client.count(request, RequestOptions.DEFAULT);
		System.out.println("--------- response----"+countResponse.getCount());
	}
	
	@Test
	public void countCombinationAPI() throws IOException {
		
		CountRequest request = new CountRequest("accesslog");
		SearchSourceBuilder searchsourceBuider = new SearchSourceBuilder();
		searchsourceBuider.query(QueryBuilders.boolQuery().
				filter(QueryBuilders.termQuery("face.age", 34)).
				filter(QueryBuilders.termQuery("face.gender", 1)));
		
		request.source(searchsourceBuider);
		
		CountResponse countResponse = client.count(request, RequestOptions.DEFAULT);
		System.out.println("--------- response----"+countResponse.getCount());
	}
	
	
	@Test
	public void fullTextSearch() throws IOException {
		
		SearchRequest request = new SearchRequest("accesslog");
		
		SearchSourceBuilder searchsourceBuider = new SearchSourceBuilder();
	}
	
	@Test
	public void rangeSearch() throws IOException {
		
		
		CountRequest request = new CountRequest("accesslog");
		SearchSourceBuilder searchsourceBuider = new SearchSourceBuilder();
		searchsourceBuider.query(QueryBuilders.rangeQuery("dateTime").from("2019-08-12 16:29:14").to("2019-08-12 16:29:33"));
		
		request.source(searchsourceBuider);
		
		CountResponse countResponse = client.count(request, RequestOptions.DEFAULT);
		System.out.println("--------- response----"+countResponse.getCount());
		
	}
	
	
	
}