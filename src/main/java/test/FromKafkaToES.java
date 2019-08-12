package test;

import java.io.IOException;

import org.apache.http.HttpHost;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.log4j.Logger;
import org.elasticsearch.action.index.IndexRequest;
import org.elasticsearch.action.index.IndexResponse;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.client.RestClient;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.common.xcontent.XContentType;

import com.sn.kafka.IKfkMessageCallback;
import com.sn.kafka.KfkConsumer;

public class FromKafkaToES {
	
	private static final Logger logger = Logger.getLogger(FromKafkaToES.class);
	
	public static void main(String[] args) throws Exception {
		
		KfkConsumer.createKfkConsumer("10.0.100.48:9193", "Capture", "Frank",1, new MessageCallback(), "admin", "Ai@9915#kafka");
	}

}

class MessageCallback implements IKfkMessageCallback {
	
	private static Logger logger = Logger.getLogger(MessageCallback.class);
	
	private static RestHighLevelClient client = new RestHighLevelClient(RestClient.builder(new HttpHost("10.100.7.111", 9200, "http")));

	@Override
	public void notify(String comsumerName, String key, String message) {
		logger.info("message-------------------"+message);
		
		IndexRequest request = new IndexRequest("accesslog");
		request.source(message, XContentType.JSON);
		try {
			IndexResponse indexResponse = client.index(request, RequestOptions.DEFAULT);
			logger.info("indexResponse is ---------"+indexResponse);
		} catch (IOException e) {
			logger.error("indexResponseError",e);
		}
	}

	@Override
	public void notify(ConsumerRecord<byte[], byte[]> record) {
		
		
	}
	
	
}
