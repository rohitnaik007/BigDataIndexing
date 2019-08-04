package com.example.demo.RedisDAO;
import javax.annotation.Resource;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.ListOperations;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class RedisDaoImpl implements RedisDao{


	@Autowired
	private RedisTemplate< String, Object > template;

	  // inject the template as ListOperations
	  @Resource(name="redisTemplate")
	  private ListOperations<String, String> listOps;

	  public void addLink(String userId, String value) {
	    listOps.leftPush(userId, value);
	    listOps.getOperations();
	    template.opsForValue().set("thing1", "thing2");
	    System.out.print(template.opsForValue().get("thing1"));
	  }

	  public Object getValue( final String key ) {
		    return template.opsForValue().get( key );
		}

		public void setValue( final String key, final String value ) {
		    template.opsForValue().set( key, value );
		}

		
		public void setObjectValue(String key, Object value) {
			template.opsForList().rightPushIfPresent(key, value);
			
		}

		public boolean deleteValue(String id) {
			return template.delete(id);
		}
	
}
