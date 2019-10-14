package com.wuxb.httpServer;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.regex.Pattern;

@SuppressWarnings("unused")
public abstract class Validate {

	private Map<String, String[]> rulesMap;
	private Map<String, String> fieldNameList;
	private Map<String, String> message;
	private Map<String, Map<String, Object>> sceneList;
	
	protected Map<String, Object> dataMap;
	private String errMessage;
	
	public Validate() {
		rulesMap = initRulesMap(setRulesMap());
		fieldNameList = setFieldNameList();
		message = setMessage();
		sceneList = setSceneList();
	}
	
	protected abstract Map<String, Object> setRulesMap();
	protected abstract Map<String, String> setFieldNameList();
	protected abstract Map<String, String> setMessage();
	protected abstract Map<String, Map<String, Object>> setSceneList();
	
	private Map<String, String[]> initRulesMap(Map<String, Object> rulesMap) {
		Map<String, String[]> newRulesMap = new LinkedHashMap<String, String[]>();
		for(Entry<String, Object> row : rulesMap.entrySet()) {
			String fieldName = row.getKey();
			Object valueObject = row.getValue();
			if(valueObject == null) {
				newRulesMap.put(fieldName, null);
			}
			else if(valueObject instanceof String) {
				String[] rules = ((String) valueObject).split("\\|");
				newRulesMap.put(fieldName, rules);
			}
			else if(valueObject instanceof String[]) {
				newRulesMap.put(fieldName, (String[]) valueObject);
			}
			else {
				System.err.println(fieldName +"的验证规则不合法");
			}
		}
		return newRulesMap;
	}
	
	public Validate scene(String action) {
		if(!sceneList.containsKey(action)) {
			System.err.println("scene动作"+ action +"不存在");
			return null;
		}
		Map<String, String[]> sceneRulesMap = initRulesMap(sceneList.get(action));
		for(Entry<String, String[]> row : sceneRulesMap.entrySet()) {
			String fieldName = row.getKey();
			String[] rules = row.getValue();
			if(rules == null) {
				if(rulesMap.containsKey(fieldName)) {
					sceneRulesMap.put(fieldName, rulesMap.get(fieldName));
				} else {
					System.err.println("rulesMap中没有字段"+ fieldName +"的规则，规则覆盖失败");
				}
				//此处只能覆盖rulesMap中的规则，不允许在sceneList里新增字段规则
			}
		}
		rulesMap = sceneRulesMap;
		return this;
	}
	
	public boolean check(Map<String, Object> dataMap) {
		if(dataMap == null) {
			return false;
		}
		this.dataMap = dataMap;
		for(Entry<String, String[]> row : rulesMap.entrySet()) {
			String fieldName = row.getKey();//字段名
			Object data = dataMap.get(fieldName);//待检测数据
			String[] rules = row.getValue();//规则
			if(rules == null || rules.length == 0) {
				continue;
			}
			//对于不要求require的且本身是空的，不再检测这个字段
			if(!Arrays.asList(rules).contains("require")) {
				if(data == null) {
					continue;
				} else if(data instanceof String) {
					if(((String) data).isEmpty()) {
						continue;
					}
				}
			}
			//对单个字段循环检查详细规则
			for(String rule : rules) {
				if(rule.isEmpty()) {
					continue;
				}
				int tempIndex = rule.indexOf(":");
				String methodName;
				String methodParam = null;
				if(tempIndex == -1) {
					methodName = rule;
				} else {
					methodName = rule.substring(0, tempIndex);
					methodParam = rule.substring(tempIndex + 1);
				}
				try {
					//查找本类中的预设方法
					Method method = Validate.class.getDeclaredMethod(methodName, Object.class, String.class);
					Object checkRes = method.invoke(this, data, methodParam);
					if(!(boolean) checkRes) {
						if(fieldNameList != null && fieldNameList.containsKey(fieldName)) {
							errMessage = fieldNameList.get(fieldName) + errMessage;
						} else {
							errMessage = fieldName + errMessage;
						}
						return false;
					}
				} catch (NoSuchMethodException e) {
					//在子类中查找自定义方法
					try {
						Method method = this.getClass().getDeclaredMethod(methodName, Object.class);
						method.setAccessible(true);
						Object checkRes = method.invoke(this, data);
						if(!(boolean) checkRes) {
							String _message = message.get(fieldName +"."+ methodName);
							if(_message == null || _message.isEmpty()) {
								_message = "验证结果错误";
							}
							if(fieldNameList != null && fieldNameList.containsKey(fieldName)) {
								errMessage = fieldNameList.get(fieldName) + _message;
							} else {
								errMessage = fieldName + _message;
							}
							return false;
						}
					} catch (NoSuchMethodException | SecurityException | IllegalAccessException | IllegalArgumentException | InvocationTargetException e1) {
						e1.printStackTrace();
						return false;
					}
				} catch (SecurityException | IllegalAccessException | IllegalArgumentException | InvocationTargetException e) {
					e.printStackTrace();
					return false;
				}
			}
		}
		return true;
	}
	
	public String getError() {
		return errMessage;
	}
	
	private boolean require(Object data, String nill) {
		if(data == null) {
			errMessage = "不得为null";
			return false;
		}
		if(data instanceof String) {
			if(((String) data).isEmpty()) {
				errMessage = "不得为空";
				return false;
			}
		}
		return true;
	}
	
	private boolean string(Object data, String nill) {
		if(!(data instanceof String)) {
			errMessage = "必须是字符串类型";
			return false;
		}
		return true;
	}
	
	private boolean integer(Object data, String nill) {
		if(!(data instanceof Long || data instanceof Integer || data instanceof Short || data instanceof Byte)) {
			errMessage = "必须是整数类型";
			return false;
		}
		return true;
	}
	
	private boolean number(Object data, String nill) {
		if(data instanceof Long || data instanceof Integer || data instanceof Short || data instanceof Byte) {
			return true;
		}
		else if(data instanceof Float || data instanceof Double) {
			return true;
		}
		else if(data instanceof String) {
			String _data = (String) data;
			if(Pattern.matches("^-?([1-9]\\d*\\.\\d*|0\\.\\d*[1-9]\\d*|0?\\.0+|0)$", _data)) {
				return true;
			}
		}
		errMessage = "必须是数字类型";
		return false;
	}
	
	private boolean min(Object data, String min) {
		int _min = Integer.parseInt(min);
		if(data instanceof Long || data instanceof Integer || data instanceof Short || data instanceof Byte) {
			long _data = Long.parseLong(data.toString());
			if(_data < _min) {
				errMessage = "值不得小于"+ _min;
				return false;
			}
		}
		else if(data instanceof String) {
			String _data = (String) data;
			if(_data.length() < _min) {
				errMessage = "长度不得小于"+ _min;
				return false;
			}
		}
		else {
			errMessage = "字段类型必须是字符串或整形数字";
			return false;
		}
		return true;
	}
	
	private boolean max(Object data, String max) {
		int _max = Integer.parseInt(max);
		if(data instanceof Long || data instanceof Integer || data instanceof Short || data instanceof Byte) {
			long _data = Long.parseLong(data.toString());
			if(_data > _max) {
				errMessage = "值不得大于"+ _max;
				return false;
			}
		}
		else if(data instanceof String) {
			String _data = (String) data;
			if(_data.length() > _max) {
				errMessage = "长度不得大于"+ _max;
				return false;
			}
		}
		else {
			errMessage = "字段类型必须是字符串或整形数字";
			return false;
		}
		return true;
	}
	
	private boolean between(Object data, String length) {
		String[] temp = length.split(",");
		int min = Integer.parseInt(temp[0]);
		int max = Integer.parseInt(temp[1]);
		if(data instanceof Long || data instanceof Integer || data instanceof Short || data instanceof Byte) {
			long _data = Long.parseLong(data.toString());
			if(_data < min) {
				errMessage = "值不得小于"+ min;
				return false;
			}
			if(_data > max) {
				errMessage = "值不得大于"+ max;
				return false;
			}
		}
		else if(data instanceof String) {
			String _data = (String) data;
			if(_data.length() < min) {
				errMessage = "长度不得小于"+ min;
				return false;
			}
			if(_data.length() > max) {
				errMessage = "长度不得大于"+ max;
				return false;
			}
		}
		else {
			errMessage = "字段类型必须是字符串或整形数字";
			return false;
		}
		return true;
	}
	
	private boolean length(Object data, String length) {
		int _length = Integer.parseInt(length);
		if(data instanceof String) {
			String _data = (String) data;
			if(_data.length() != _length) {
				errMessage = "长度必须等于"+ length;
				return false;
			}
		}
		else {
			errMessage = "字段类型必须是字符串";
			return false;
		}
		return true;
	}
	
	private boolean regex(Object data, String regex) {
		if(!(data instanceof String)) {
			errMessage = "字段类型必须是字符串";
			return false;
		}
		if(!Pattern.matches(regex, (String) data)) {
			errMessage = "字段格式错误";
			return false;
		}
		return true;
	}
	
	private boolean email(Object data, String nill) {
		if(!(data instanceof String)) {
			errMessage = "字段类型必须是字符串";
			return false;
		}
		if(!Pattern.matches("^[_a-z0-9-]+(\\.[_a-z0-9-]+)*@[a-z0-9-]+(\\.[a-z0-9-]+)*(\\.[a-z]{2,})$", (String) data)) {
			errMessage = "字段格式必须是电子邮箱";
			return false;
		}
		return true;
	}
	
	private boolean mobile(Object data, String nill) {
		if(!(data instanceof String)) {
			errMessage = "字段类型必须是字符串";
			return false;
		}
		if(!Pattern.matches("^1[3-9]{1}{0-9}{9}$", (String) data)) {
			errMessage = "字段格式必须是电子邮箱";
			return false;
		}
		return true;
	}
	
	private boolean url(Object data, String nill) {
		if(!(data instanceof String)) {
			errMessage = "字段类型必须是字符串";
			return false;
		}
		if(!Pattern.matches("^(https?|ftp|file)://[-a-zA-Z0-9+&@#/%?=~_|!:,.;]*[-a-zA-Z0-9+&@#/%=~_|]", (String) data)) {
			errMessage = "字段格式必须是url";
			return false;
		}
		return true;
	}
	
}
