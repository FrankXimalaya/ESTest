package test;

import java.io.IOException;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
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
import org.elasticsearch.action.search.SearchResponse;
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
import org.elasticsearch.search.aggregations.AggregationBuilders;
import org.elasticsearch.search.aggregations.Aggregations;
import org.elasticsearch.search.aggregations.bucket.terms.Terms;
import org.elasticsearch.search.aggregations.bucket.terms.TermsAggregationBuilder;
import org.elasticsearch.search.aggregations.metrics.Avg;
import org.elasticsearch.search.aggregations.metrics.Stats;
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

		System.setProperty("http.proxySet", "true");
		System.setProperty("http.proxyHost", "127.0.0.1");
		System.setProperty("http.proxyPort", "8888");

		client = new RestHighLevelClient(RestClient.builder(new HttpHost("10.100.7.111", 9200, "http")));
	}

	/**
	 * ��String�ķ�ʽ��������
	 */
	@Test
	public void indexString() throws IOException {
		IndexRequest request = new IndexRequest("first_index");
		request.id("1");

		String jsonString = "{" + "\"user\":\"frank\"," + "\"postDate\":\"2018-08-08\","
				+ "\"message\":\"trying out Elasticsearch\"" + "}";
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
		IndexRequest indexRequest = new IndexRequest("first_index").id("eagles").source(jsonMap).setIfSeqNo(5)
				.setIfPrimaryTerm(1);

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
		IndexRequest indexRequest = new IndexRequest("first_index").id("3").source(jsonMap).setIfSeqNo(5)
				.setIfPrimaryTerm(1);

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
				logger.error("ERROR", e);
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

		GetRequest request = new GetRequest("first_index", "11");

		GetResponse getResponse = client.get(request, RequestOptions.DEFAULT);
		System.out.println(JSONObject.toJSON(getResponse));
	}

	/**
	 * �ж��Ƿ����,�����get��ֻ࣬��������
	 */
	@Test
	public void judgeExist() {

		GetRequest getRequest = new GetRequest("posts", "1");
		getRequest.fetchSourceContext(new FetchSourceContext(false));
		getRequest.storedFields("_none_");
	}

	/***
	 * ɾ��API
	 * 
	 * @throws IOException
	 */
	@Test
	public void delete() throws IOException {

		DeleteRequest request = new DeleteRequest("first_index", "4");
		DeleteResponse deleteResponse = client.delete(request, RequestOptions.DEFAULT);

		System.out.println(JSONObject.toJSON(deleteResponse));
	}

	/***
	 * ���� ,�м�����ʽ��ֱ�Ӵ�JSON���ߴ�Map,����дscript
	 * 
	 * @throws IOException
	 */
	@Test
	public void update() throws IOException {

		Map<String, Object> map = new HashMap<String, Object>();
		map.put("age", 100000);
		UpdateRequest request = new UpdateRequest("first_index", "6").doc(map);
		client.update(request, RequestOptions.DEFAULT);

	}

	/***
	 * ����ĳ���ֶ�
	 * 
	 * @throws IOException
	 */
	@Test
	public void updateFieldMap() throws IOException {

		UpdateRequest request = new UpdateRequest("accesslog", "zy-OiWwB_oPNGbRdACue");
		Map<String, Object> map = new HashMap<String, Object>();
		map.put("face.age", 100);
		request.doc(map);
		System.out.println(JSONObject.toJSON(client.update(request, RequestOptions.DEFAULT)));
//		
//		UpdateByQueryRequestBuilder builder = new UpdateByQueryRequestBuilder();
//		
////		UpdateRequest request = new UpdateRequest("accesslog","yy-NiWwB_oPNGbRd_yub").doc(map);
//		client.update(request, RequestOptions.DEFAULT);

	}

	/***
	 * ����ĳ���ֶ�ͨ��JSON,ͨ�����ַ�ʽ���оֲ�����
	 * 
	 * @throws IOException
	 */
	@Test
	public void updateFieldJson() throws IOException {

		UpdateRequest request = new UpdateRequest("accesslog", "zy-OiWwB_oPNGbRdACue");

		JSONObject faceJsonObject = new JSONObject();
		faceJsonObject.put("feature", "frank");

		JSONObject jsonObject = new JSONObject();
		jsonObject.put("face", faceJsonObject);
		request.doc(jsonObject, XContentType.JSON);
		System.out.println(JSONObject.toJSON(client.update(request, RequestOptions.DEFAULT)));
//		
//		UpdateByQueryRequestBuilder builder = new UpdateByQueryRequestBuilder();
//		
////		UpdateRequest request = new UpdateRequest("accesslog","yy-NiWwB_oPNGbRd_yub").doc(map);
//		client.update(request, RequestOptions.DEFAULT);

	}

	@Test
	public void bulkIndex() throws IOException {

		BulkRequest request = new BulkRequest();
		request.add(new IndexRequest("first_index").id("1").source(XContentType.JSON, "field", "foo"));
		request.add(new IndexRequest("first_index").id("2").source(XContentType.JSON, "field", "bar"));
		request.add(new IndexRequest("first_index").id("3").source(XContentType.JSON, "field", "baz"));

		BulkResponse bulkResponse = client.bulk(request, RequestOptions.DEFAULT);

		// ����ط���װͦţ�Ƶģ�ʵ����iteratable�ӿڣ���ֱ�ӿ�����forѭ����
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
		request.add(new MultiGetRequest.Item("first_index", "1"));
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
		System.out.println("--------- response----" + countResponse.getCount());
	}

	@Test
	public void countConditionAPI() throws IOException {

		CountRequest request = new CountRequest("accesslog");
		SearchSourceBuilder searchsourceBuider = new SearchSourceBuilder();
		searchsourceBuider.query(QueryBuilders.termQuery("face.age", 64));

		request.source(searchsourceBuider);

		CountResponse countResponse = client.count(request, RequestOptions.DEFAULT);
		System.out.println("--------- response----" + countResponse.getCount());
	}

	@Test
	public void countCombinationAPI() throws IOException {

		CountRequest request = new CountRequest("accesslog");
		SearchSourceBuilder searchsourceBuider = new SearchSourceBuilder();
		searchsourceBuider.query(QueryBuilders.boolQuery().filter(QueryBuilders.termQuery("face.age", 34))
				.filter(QueryBuilders.termQuery("face.gender", 1)));

		request.source(searchsourceBuider);

		CountResponse countResponse = client.count(request, RequestOptions.DEFAULT);
		System.out.println("--------- response----" + countResponse.getCount());
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
		System.out.println("--------- response----" + countResponse.getCount());
	}

	@Test
	public void rangeSearchDateTime() throws IOException {

		SearchSourceBuilder searchsourceBuider = new SearchSourceBuilder();
		searchsourceBuider
				.query(QueryBuilders.rangeQuery("dateTime").gt("2019-08-12 17:57:27").lt("2019-08-12 17:57:32"));

//		searchsourceBuider.query(QueryBuilders.rangeQuery("dateTime").gt(DateHelper.strToDateLong("2019-08-12 16:29:14"))
//				.lt(DateHelper.strToDateLong("2019-08-12 16:29:33")));

		CountRequest request = new CountRequest("accesslog");
		request.source(searchsourceBuider);

		CountResponse countResponse = client.count(request, RequestOptions.DEFAULT);
		System.out.println("--------- response----" + countResponse.getCount());
	}

	/** ������ȷ���� */
	@Test
	public void AccurateSearch() throws IOException {

		SearchSourceBuilder searchsourceBuider = new SearchSourceBuilder();
//		searchsourceBuider.query(QueryBuilders.termQuery("face.feature", "HgAHDA36AQAACR7qAf/6/A4DDwoCEPzt/f78B/wCCf378gAACQId+fr0BAf+7QP2AwAM+wQS+e4GCAgGBfsF9vwC7QghCQAi/u8Y9v79HgwN5fAAB/UL/BkHBRYA7g3o5goJDPkQ9iD/AP/6AfsA/PYBEfr8AxDzBQDwCPYA+hv2HPHwC/X1CPfy/gII9RQM8/AB9woNB/32BPsABxQG9u4AEgXrAwgJCAoUCA3+CggUAfD8/PIC+xL27fYPAQwF9+cX/vf8AwYNB/4TBQb2/wwO9hQE8Q0XBPgRABr78wAADBMACf/tCuT7EvcT+g0OBPgABvwI4AAHAPT3APsD7xH+/vH++AP6/Q34/QsABRXy6fcBBeQCBAAHBAYGBPH17wkCDP8G6+YAAP3j+/31AfoHFP3xB/oC9gX6ABDu+vkUBgz0BO3x//z16AsADe4DFiMvEwIF8v8L7fcTAf0PFAH64Q0J/hLt/u8K5wAH+wcADxPy/hLxBwj4+Q4BFvEYkgJ5Q9WXgzs="));

		searchsourceBuider.query(QueryBuilders.boolQuery().filter(QueryBuilders.termQuery("face.feature.keyword",
				"9/4T6QAa+/r2//wA5Pbx/wkAB/D5Cv335vv4EO7/Bgr98v0IBwAD9QEE8Qka8A/+CgoN+vn27wjwAAzx8vYQA/IV8/b6CvH2Bw4fBOn9AgD6Bwv7Ae/38wEF7wIAGwEG/PsDCvf+BwPmA+UICg0AAPD3Bwz27QcJB/cCEBwM+eQK+/gHBeX9AQAd5+0E+QgaA/wA+xUJ//8QA/kH9P0C/xEHGw/yFQMBFA4AAxcB+f8F/yD3CwQIABLx/BYF5xgB/AH5EPvr/f4Q+BEG8wD0E+sF/AL/4/4GAPsT9vkC/QoC9OcB7vn5+AkQEgLw7AUb6hEADQ4OCxAF/wIQGAX/Cvb87v748gT5AwAREggJDvEL/RHz/PkZAgHj/wQACuoL9f75+QwM9wEK+Rru9hEI+wHyA/b1+QjvDfoUA+8R8ekIA/f0BAf4DfUCAAIM8RMF8iT8A/kB6A0E7u8PCgr79uoE+fgM6/gNEAoD+ewR/Qjp8wMN6Oz27wsBAf74FgQTQlp4QwPxgzs=")));

		CountRequest request = new CountRequest("accesslog");
		request.source(searchsourceBuider);

		CountResponse countResponse = client.count(request, RequestOptions.DEFAULT);
		System.out.println("--------- response----" + countResponse.getCount());
	}

	/** ʱ�侫ȷ���� */
	@Test
	public void TimeSearch() throws IOException {

		SearchSourceBuilder searchsourceBuider = new SearchSourceBuilder();
		searchsourceBuider
				.query(QueryBuilders.boolQuery().filter(QueryBuilders.termQuery("dateTime", "2019-08-12 17:48:11")));

		CountRequest request = new CountRequest("accesslog");
		request.source(searchsourceBuider);

		CountResponse countResponse = client.count(request, RequestOptions.DEFAULT);
		System.out.println("--------- response----" + countResponse.getCount());
	}

	/**
	 * ȫ�ļ���
	 * 
	 * @throws IOException
	 */
	@Test
	public void fullTextQuery() throws IOException {

		SearchSourceBuilder searchsourceBuider = new SearchSourceBuilder();
		searchsourceBuider.query(QueryBuilders.matchQuery("message", "Elasticsearch out"));

		CountRequest request = new CountRequest("twitter");
		request.source(searchsourceBuider);

		QueryBuilders.matchPhraseQuery("message", "trying out");

		CountResponse countResponse = client.count(request, RequestOptions.DEFAULT);
		System.out.println("--------- response----" + countResponse.getCount());
	}

	/**
	 * �������
	 * 
	 * @throws IOException
	 */
	@Test
	public void multitextQuery() throws IOException {

		SearchSourceBuilder searchsourceBuider = new SearchSourceBuilder();
		searchsourceBuider.query(QueryBuilders.matchPhraseQuery("message", "����"));

		CountRequest request = new CountRequest("twitter");
		request.source(searchsourceBuider);

		CountResponse countResponse = client.count(request, RequestOptions.DEFAULT);
		System.out.println("--------- response----" + countResponse.getCount());
	}

	/**
	 * ǰ׺����
	 * 
	 * @throws IOException
	 */
	@Test
	public void prefixQuery() throws IOException {

		SearchSourceBuilder searchsourceBuider = new SearchSourceBuilder();
		searchsourceBuider.query(QueryBuilders.prefixQuery("message", "el"));

		CountRequest request = new CountRequest("twitter");
		request.source(searchsourceBuider);

		CountResponse countResponse = client.count(request, RequestOptions.DEFAULT);
		System.out.println("--------- response----" + countResponse.getCount());
	}

	/**
	 * ģ������
	 * 
	 * @throws IOException
	 */
	@Test
	public void fuzzyTextQuery() throws IOException {

		SearchSourceBuilder searchsourceBuider = new SearchSourceBuilder();
		searchsourceBuider.query(QueryBuilders.fuzzyQuery("face.feature", "dayacj1fgyfbp4q9pusb"));

		SearchRequest request = new SearchRequest("accesslog");
		request.source(searchsourceBuider);

		SearchResponse searchResponse = client.search(request, RequestOptions.DEFAULT);
		System.out.println("--------- response----" + JSONObject.toJSONString(searchResponse));
	}

	/**
	 * ͨ���
	 * 
	 * @throws IOException
	 */
	@Test
	public void wildcardQuery() throws IOException {

		SearchSourceBuilder searchsourceBuider = new SearchSourceBuilder();
		searchsourceBuider.query(QueryBuilders.wildcardQuery("face.feature", "*CAH6CBIJ+wH8DP8H8RIICyAF*"));

		CountRequest request = new CountRequest("accesslog");
		request.source(searchsourceBuider);

		CountResponse countResponse = client.count(request, RequestOptions.DEFAULT);
		System.out.println("--------- response----" + countResponse.getCount());
	}

	/**
	 * ����������ṹ��������ȫ�ļ�����һ��������
	 * 
	 * @throws IOException
	 */
	@Test
	public void comboundSearch() throws IOException {

		SearchSourceBuilder searchsourceBuider = new SearchSourceBuilder();
		searchsourceBuider.query(QueryBuilders.boolQuery().must(QueryBuilders.matchQuery("message", "��"))
				.must(QueryBuilders.termQuery("age", 18)));

		CountRequest request = new CountRequest("twitter");
		request.source(searchsourceBuider);

		CountResponse countResponse = client.count(request, RequestOptions.DEFAULT);
		System.out.println("--------- response----" + countResponse.getCount());

	}

	/**
	 * �򵥾ۺ�,Ů�Ե�ƽ������
	 * 
	 * @throws IOException
	 */
	@Test
	public void easyAgregation() throws IOException {

		SearchRequest searchRequest = new SearchRequest("twitter");

		SearchSourceBuilder searchSourceBuilder = new SearchSourceBuilder();

		// ȡ���֣��Լ������ĸ��ֶ�
		TermsAggregationBuilder aggregation = AggregationBuilders.terms("by_sex").field("sex"); // text���Ͳ����������������򣬱���ת��keyword����

		// ������һ��Ƕ�ײ�ѯ��Ӧ�����ȸ����Ա���з�Ͱ�����Ա��Ͱ�Ļ������ٽ�������ľۺ�
		aggregation.subAggregation(AggregationBuilders.avg("avg_age").field("age")); // avg_age Ϊ�Ӿۺ����ƣ����ƿ�����

		searchSourceBuilder.aggregation(aggregation);
		searchRequest.source(searchSourceBuilder);
		SearchResponse searchResponse = null;
		try {
			searchResponse = client.search(searchRequest, RequestOptions.DEFAULT);
		} catch (IOException e) {
			e.printStackTrace();
		}
		Aggregations aggregations = searchResponse.getAggregations();

		Terms bySexAggregation = aggregations.get("by_sex");

		Terms.Bucket elasticBucket = bySexAggregation.getBucketByKey("1");

		Avg averageAge = elasticBucket.getAggregations().get("avg_age");

		double avg = averageAge.getValue();

		System.out.println("Ů��ƽ�����䣺" + avg);
	}

	/**
	 * ͳ�����ֵ����Сֵ��
	 */
	@Test
	public void statistics() {

		logger.info("����git merge����");

		SearchRequest searchRequest = new SearchRequest();
		SearchSourceBuilder searchSourceBuilder = new SearchSourceBuilder();
		
		TermsAggregationBuilder aggregation = AggregationBuilders.terms("by_sex").field("sex"); // by_sexΪ�ۺ����ƣ����ƿ�����
		
		aggregation.subAggregation(AggregationBuilders.stats("stat_age").field("age")); // avg_age Ϊ�Ӿۺ����ƣ����ƿ�����
		
		searchSourceBuilder.aggregation(aggregation);
		searchRequest.source(searchSourceBuilder);
		SearchResponse searchResponse = null;
		
		try {
			searchResponse = client.search(searchRequest, RequestOptions.DEFAULT);
		} catch (IOException e) {
			e.printStackTrace();
		}
		Aggregations aggregations = searchResponse.getAggregations();
		Terms bySexAggregation = aggregations.get("by_sex");
		List<? extends Terms.Bucket> buckets = bySexAggregation.getBuckets();
		for (Terms.Bucket bucket : buckets) {
			Stats statAge = bucket.getAggregations().get("stat_age");
			System.out.println(bucket.getKey()); // ��ȡ��������
			System.out.println("ƽ��ֵ��" + statAge.getAvg());
			System.out.println("������" + statAge.getSum());
			System.out.println("���ֵ��" + statAge.getMaxAsString());
			System.out.println("��Сֵ��" + statAge.getMin());
		}

	}

}