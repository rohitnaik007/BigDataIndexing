package com.example.demo.RedisDAO;

public interface RedisDao {
	 public void addLink(String userId, String value);
	 
	 public Object getValue( final String key );
		public void setValue( final String key, final String value );
		
		public void setObjectValue( final String key, final Object value );
		
		public boolean deleteValue(String id);
}

