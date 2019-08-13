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
import org.elasticsearch.index.reindex.UpdateByQueryRequest;
import org.elasticsearch.index.reindex.UpdateByQueryRequestBuilder;
import org.elasticsearch.search.builder.SearchSourceBuilder;
import org.elasticsearch.search.fetch.subphase.FetchSourceContext;
import org.junit.Before;
import org.junit.Test;

import com.alibaba.fastjson.JSONObject;



/**
 * 
 * 以下版本都是基于7.1版本，属于HighLevel
 */
public class EsTest {

	private static RestHighLevelClient client;
	
	private static final Logger logger = Logger.getLogger(EsTest.class);

	@Before
	public void initialize() {

		client = new RestHighLevelClient(RestClient.builder(new HttpHost("10.100.7.111", 9200, "http")));
	}
	
	/**
	 * 以String的方式插入数据
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
	 * 以Map的形式插入,如果ID相同就是更新
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
	 * 异步的方式进行更新 
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
		    	System.out.println("请求失败");
		    }
		};
		client.indexAsync(indexRequest, RequestOptions.DEFAULT, actionListener);
		Thread.sleep(10000);
	}
	/**
	 * 查询获取 
	 */
	@Test
	public void getRequest() throws IOException {
		
		GetRequest request = new GetRequest( "first_index", "11");
		
		GetResponse getResponse = client.get(request, RequestOptions.DEFAULT);		
		System.out.println(JSONObject.toJSON(getResponse));
	}	
	/**
	 * 判断是否存在,这个和get差不多，只不过更轻
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
     * 删除API  
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
      * 更新  
	 * @throws IOException 
	 */
	@Test
	public void update() throws IOException {
		
		Map<String, Object> map = new HashMap<String, Object>();
		map.put("age", 100000);
		UpdateRequest request = new UpdateRequest("first_index","6").doc(map);
		client.update(request, RequestOptions.DEFAULT);
		
	}
	

	/***
      * 更新某个字段
	 * @throws IOException 
	 */
	@Test
	public void updateField() throws IOException {
		
		UpdateByQueryRequest request = new UpdateByQueryRequest("accesslog");
		request.setQuery(QueryBuilders.idsQuery().addIds("yy-NiWwB_oPNGbRd_yub"));
		
//		
//		UpdateByQueryRequestBuilder builder = new UpdateByQueryRequestBuilder();
//		
////		UpdateRequest request = new UpdateRequest("accesslog","yy-NiWwB_oPNGbRd_yub").doc(map);
//		client.update(request, RequestOptions.DEFAULT);
		
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
		
		//这个地方封装挺牛逼的，实现了iteratable接口，就直接可以在for循环里
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
		
//		QueryBuilders.rangeQuery("").gt("").
		
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
		
		SearchSourceBuilder searchsourceBuider = new SearchSourceBuilder();
//		searchsourceBuider.query(QueryBuilders.rangeQuery("dateTime").gt("2019-08-12 16:29:14").lt("2019-08-12 16:29:33"));
		
		searchsourceBuider.query(QueryBuilders.rangeQuery("face.confidence").gt(97.4f));
		
//		searchsourceBuider.query(QueryBuilders.rangeQuery("dateTime").gt(DateHelper.strToDateLong("2019-08-12 16:29:14"))
//				.lt(DateHelper.strToDateLong("2019-08-12 16:29:33")));
		
		
		CountRequest request = new CountRequest("accesslog");
		request.source(searchsourceBuider);
		
		CountResponse countResponse = client.count(request, RequestOptions.DEFAULT);
		System.out.println("--------- response----"+countResponse.getCount());
	}
	
	
	@Test
	public void rangeSearchDateTime() throws IOException {
		
		SearchSourceBuilder searchsourceBuider = new SearchSourceBuilder();
		searchsourceBuider.query(QueryBuilders.rangeQuery("dateTime").gt("2019-08-12 17:57:27").lt("2019-08-12 17:57:32"));
		
//		searchsourceBuider.query(QueryBuilders.rangeQuery("dateTime").gt(DateHelper.strToDateLong("2019-08-12 16:29:14"))
//				.lt(DateHelper.strToDateLong("2019-08-12 16:29:33")));
		
		CountRequest request = new CountRequest("accesslog");
		request.source(searchsourceBuider);
		
		CountResponse countResponse = client.count(request, RequestOptions.DEFAULT);
		System.out.println("--------- response----"+countResponse.getCount());
	}
	
	/**特征精确查找*/
	@Test
	public void AccurateSearch() throws IOException {
		
		SearchSourceBuilder searchsourceBuider = new SearchSourceBuilder();
//		searchsourceBuider.query(QueryBuilders.termQuery("face.feature", "HgAHDA36AQAACR7qAf/6/A4DDwoCEPzt/f78B/wCCf378gAACQId+fr0BAf+7QP2AwAM+wQS+e4GCAgGBfsF9vwC7QghCQAi/u8Y9v79HgwN5fAAB/UL/BkHBRYA7g3o5goJDPkQ9iD/AP/6AfsA/PYBEfr8AxDzBQDwCPYA+hv2HPHwC/X1CPfy/gII9RQM8/AB9woNB/32BPsABxQG9u4AEgXrAwgJCAoUCA3+CggUAfD8/PIC+xL27fYPAQwF9+cX/vf8AwYNB/4TBQb2/wwO9hQE8Q0XBPgRABr78wAADBMACf/tCuT7EvcT+g0OBPgABvwI4AAHAPT3APsD7xH+/vH++AP6/Q34/QsABRXy6fcBBeQCBAAHBAYGBPH17wkCDP8G6+YAAP3j+/31AfoHFP3xB/oC9gX6ABDu+vkUBgz0BO3x//z16AsADe4DFiMvEwIF8v8L7fcTAf0PFAH64Q0J/hLt/u8K5wAH+wcADxPy/hLxBwj4+Q4BFvEYkgJ5Q9WXgzs="));
		
		searchsourceBuider.query(QueryBuilders.boolQuery().filter(QueryBuilders.termQuery("face.feature", "HgAHDA36AQAACR7qAf/6/A4DDwoCEPzt/f78B/wCCf378gAACQId+fr0BAf+7QP2AwAM+wQS+e4GCAgGBfsF9vwC7QghCQAi/u8Y9v79HgwN5fAAB/UL/BkHBRYA7g3o5goJDPkQ9iD/AP/6AfsA/PYBEfr8AxDzBQDwCPYA+hv2HPHwC/X1CPfy/gII9RQM8/AB9woNB/32BPsABxQG9u4AEgXrAwgJCAoUCA3+CggUAfD8/PIC+xL27fYPAQwF9+cX/vf8AwYNB/4TBQb2/wwO9hQE8Q0XBPgRABr78wAADBMACf/tCuT7EvcT+g0OBPgABvwI4AAHAPT3APsD7xH+/vH++AP6/Q34/QsABRXy6fcBBeQCBAAHBAYGBPH17wkCDP8G6+YAAP3j+/31AfoHFP3xB/oC9gX6ABDu+vkUBgz0BO3x//z16AsADe4DFiMvEwIF8v8L7fcTAf0PFAH64Q0J/hLt/u8K5wAH+wcADxPy/hLxBwj4+Q4BFvEYkgJ5Q9WXgzs=")));
		
		CountRequest request = new CountRequest("accesslog");
		request.source(searchsourceBuider);
		
		CountResponse countResponse = client.count(request, RequestOptions.DEFAULT);
		System.out.println("--------- response----"+countResponse.getCount());
	}
	
	/**时间精确查找*/
	@Test  
	public void TimeSearch() throws IOException {
		
		SearchSourceBuilder searchsourceBuider = new SearchSourceBuilder();
		searchsourceBuider.query(QueryBuilders.boolQuery().
				filter(QueryBuilders.termQuery("dateTime", "2019-08-12 17:48:11")));
		
		CountRequest request = new CountRequest("accesslog");
		request.source(searchsourceBuider);
		
		CountResponse countResponse = client.count(request, RequestOptions.DEFAULT);
		System.out.println("--------- response----"+countResponse.getCount());
	}
	
	
	/**全文检索
	 * @throws IOException */
	@Test
	public void fullTextQuery() throws IOException {
		
		SearchSourceBuilder searchsourceBuider = new SearchSourceBuilder();
		searchsourceBuider.query(QueryBuilders.matchQuery("personId", "{\"arrrrrreaId\":\"000003\",\"cameraName\":\"10.0.10.203\"}"));
		
		CountRequest request = new CountRequest("accesslog");
		request.source(searchsourceBuider);
		
		CountResponse countResponse = client.count(request, RequestOptions.DEFAULT);
		System.out.println("--------- response----"+countResponse.getCount());
	}	
}