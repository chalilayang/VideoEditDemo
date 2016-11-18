package com.chalilayang.mediaextractordemo.Utils;

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.text.DecimalFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.text.TextUtils;
import android.view.ViewTreeObserver;
import android.view.ViewTreeObserver.OnGlobalLayoutListener;
import android.widget.TextView;

public class StringUtil {

	/**
	 * 返回 format 格式的时间字符串 时间格式为 yyyy-MM-dd HH:mm:ss yyyy 返回4位年份 MM 返回2位月份 dd
	 * 返回2位日 时间类同
	 *
	 * @return 相应日期类型的字符串
	 */
	public static String getCurrnetDate(String format) {
		SimpleDateFormat sdf = new SimpleDateFormat(format);
		return sdf.format(new Date()).toString();
	}

	/**
	 * 获取整除结果
	 *
	 * @param itemNum
	 * @param allVoteNum
	 * @return
	 */
	public static float getPercent(int itemNum, long allVoteNum) {
		float p = (float) itemNum / allVoteNum;

		DecimalFormat df = new DecimalFormat("0.00");// 格式化小数，不足的补0
		String s = df.format(p);
		float f = Float.parseFloat(s);

		return f;
	}

	/**
	 * 获取当前时间的 小时和秒
	 *
	 * @param date
	 * @return
	 */
	public static String getHourSecond(Date date) {

		String fomat = "yyyy-MM-dd HH:mm:ss";
		SimpleDateFormat dfs = new SimpleDateFormat(fomat);
		String rst = dfs.format(date);

		return rst.substring(10, 15);
	}

	/**
	 * 获取当前时间的 小时
	 *
	 * @param date
	 * @return
	 */
	public static String getHour(Date date) {

		String fomat = "yyyy-MM-dd HH:mm:ss";
		SimpleDateFormat dfs = new SimpleDateFormat(fomat);
		String rst = dfs.format(date);

		return rst.substring(11, 13);
	}

	/**
	 * 获取何时已经刷新的时间
	 *
	 * @param refreshTime
	 * @return
	 */
	public static String getDateForRefresh(long refreshTime) {
		String date = "";
		try {
			Date begin = new Date(refreshTime);
			Date end = new Date();
			long between = (end.getTime() - begin.getTime()) / 1000;// 除以1000是为了转换成秒

			long day = between / (24 * 3600);
			long hour = between % (24 * 3600) / 3600;
			long minute = between % 3600 / 60;
			long second = between % 60 / 60;

			if (day >= 1) {
				date = new SimpleDateFormat("MM月dd日").format(begin);
			} else if (hour > 0 && day == 0) {
				date = hour + "小时前";
			} else if (minute > 0 && hour == 0 && day == 0) {
				date = minute + "分钟前";
			}/*
			 * else if(second>0 && minute==0&&hour==0 && day == 0){ date =
			 * second + "秒前"; }
			 */else {
				date = "1分钟前";
			}

		} catch (Exception e) {
			e.printStackTrace();
		}
		if (!TextUtils.isEmpty(date)) {
			return date + "更新";
		}
		return "";
	}

	/**
	 * 获取时间差
	 *
	 * @param date
	 * @return
	 */
	public static String getDate(String date) {
		if (null == date || "".equals(date)) {
			return "";
		}
		String fomat = "yyyy-MM-dd HH:mm:ss";
		try {
			SimpleDateFormat dfs = new SimpleDateFormat(fomat);
			Date begin = dfs.parse(date);
			Date end = dfs.parse(StringUtil.getCurrnetDate(fomat));
			long between = (end.getTime() - begin.getTime()) / 1000;// 除以1000是为了转换成秒

			long day = between / (24 * 3600);
			long hour = between % (24 * 3600) / 3600;
			long minute = between % 3600 / 60;
			long second = between % 60 / 60;

			if (day >= 1) {
				date = new SimpleDateFormat("yyyy-MM-dd").format(begin);
			} else if (hour > 0 && day == 0) {
				date = hour + "小时前";
			} else if (minute > 0 && hour == 0 && day == 0) {
				date = minute + "分钟前";
			}/*
			 * else if(second>0 && minute==0&&hour==0 && day == 0){ date =
			 * second + "秒前"; }
			 */else {
				date = "1分钟前";
			}

		} catch (ParseException e) {
			e.printStackTrace();
		}

		return date;
	}

	/**
	 * 获取时间差
	 *
	 * @param date
	 * @param format
	 * @return
	 */
	public static String getDate(String date, String format) {
		try {
			SimpleDateFormat dfs = new SimpleDateFormat(format);
			Date begin = dfs.parse(date);
			Date end = dfs.parse(StringUtil.getCurrnetDate(format));
			long between = (end.getTime() - begin.getTime()) / 1000;// 除以1000是为了转换成秒

			long day = between / (24 * 3600);
			long hour = between % (24 * 3600) / 3600;
			long minute = between % 3600 / 60;
			long second = between % 60 / 60;

			if (day >= 1) {
				date = new SimpleDateFormat("yyyy-MM-dd").format(begin);
			} else if (hour > 0 && day == 0) {
				date = hour + "小时前";
			} else if (minute > 0 && hour == 0 && day == 0) {
				date = minute + "分钟前";
			} else if (second > 0 && minute == 0 && hour == 0 && day == 0) {
				date = second + "秒前";
			}

		} catch (ParseException e) {
			e.printStackTrace();
		}
		return date;
	}

	/**
	 * 时间大小比较
	 *
	 * @param endDate
	 * @param beginDate
	 * @return
	 */
	public static boolean compareDate(String endDate, String beginDate) {
		String fomat = "yyyy-MM-dd HH:mm:ss";
		try {
			SimpleDateFormat dfs = new SimpleDateFormat(fomat);
			Date begin = dfs.parse(beginDate);
			Date end = dfs.parse(endDate);
			long between = (end.getTime() - begin.getTime());

			if (between > 0) {
				return true;
			}

		} catch (ParseException e) {
			e.printStackTrace();
		}

		return false;
	}

	/**
	 * socket 异常处理
	 *
	 * @param str
	 * @return
	 */
	public static String replaceEnter(String str) {
		str = str.replace("'", "");
		return str.replace('\n', ' ');
	}

	public static String getString(Object parm) {
		if (parm != null) {
			return parm.toString();
		} else
			return "";
	}

	/**
	 * Json 转换为 String
	 *
	 * @param json
	 * @return String
	 */
	public static String JsonToString(JSONObject json) {

		if (null != json) {

			return json.toString();
		} else {

			return "";
		}

	}

	/**
	 * String 转换为 Json
	 *
	 * @param str
	 * @return Json
	 */
	public static JSONObject StringToJson(String str) {

		if (null != str) {
			try {
				JSONObject json = new JSONObject(str);
				return json;
			} catch (JSONException e) {
				return null;
			}
		} else {

			return null;
		}
	}

	/**
	 * String 转换为 JSONArray
	 *
	 * @param str
	 * @return JSONArray
	 */
	public static JSONArray StringToJsonArray(String str) {

		if (null != str) {
			try {
				JSONArray jsonArray = new JSONArray(str);
				return jsonArray;
			} catch (JSONException e) {
				return null;
			}
		} else {

			return null;
		}
	}

	/**
	 * 查找 regex 在 string 中出现的次数
	 *
	 * @param string
	 * @param regex
	 */
	public static int findRegexCount(StringBuilder string, String regex) {

		Pattern pt = Pattern.compile(regex, Pattern.CASE_INSENSITIVE);
		Matcher mc = pt.matcher(string);
		int count = 0;
		while (mc.find()) {
			++count;
		}
		if (count % 2 != 0)
			return count;

		replaceSpace(string, regex);

		return count;
	}

	public static void replaceSpace(StringBuilder str, String tag) {
		Matcher mc = Pattern.compile(tag).matcher(str);
		mc = Pattern.compile(tag).matcher(str);
		while (mc.find()) {
			int index;
			try {
				index = mc.start();
			} catch (Exception e1) {
				break;
			}

			while (true) {
				try {
					if (--index < 0)
						break;

					char s = str.charAt(index);

					boolean isspace = isSpaceChar(s);

					if (isspace) {
						str.replace(index, index + 1, "");
						mc = Pattern.compile(tag).matcher(str);
					} else {
						break;
					}
				} catch (Exception e) {
					e.printStackTrace();
					break;
				}

			}

		}
	}

	/**
	 * 判断空白字符串
	 *
	 * @param str
	 * @return
	 */
	private static boolean isSpaceChar(char str) {
		String regex = "^\\s*$";
		Pattern pt = Pattern.compile(regex, Pattern.CASE_INSENSITIVE);
		Matcher mac = pt.matcher(String.valueOf(str));
		return mac.find();
	}

	/**
	 * 判斷空字符串
	 *
	 * @param str
	 * @return
	 */
	public static boolean isEmptyStr(String str) {
		return null == str || "".equals(str.trim());
	}
	
	public static boolean isNotEmptyStr(String str) {
	    return !isEmptyStr(str);
	}

	/**
	 * 验证邮箱地址是否合法
	 *
	 * @param email
	 * @return
	 */
	public static boolean validEmail(String email) {
		if(isEmptyStr(email)){
			return false;
		}
		String check = "^([a-z0-9A-Z]+[-|\\.]?)+[a-z0-9A-Z]@([a-z0-9A-Z]+(-[a-z0-9A-Z]+)?\\.)+[a-zA-Z]{2,}$";
		Pattern regex = Pattern.compile(check);
		Matcher matcher = regex.matcher(email);
		return matcher.matches();
	}

	/**
	 * 验证电话号码是否合法
	 *
	 * @param phone
	 * @return
	 */
	public static boolean validPhone(String phone)  {
		if(isEmptyStr(phone)){
			return false;
		}
		String check = "^(13[0-9]|15[0-9]|18[0,1,2,3,6,7,8,9])\\d{8}$";
		Pattern regex = Pattern.compile(check);
		Matcher matcher = regex.matcher(phone);
		return matcher.matches();
	}

	public static String formatTime(int ms)
    {
        if(ms < 0)
            return "";
        int sec = ms / 1000;
        int hour = sec/3600;
        int minute = (sec%3600)/60;
        sec = sec%3600%60;
        StringBuilder builder = new StringBuilder();
        if(hour > 0) {
            if(hour < 10){
                builder.append(0);
            }
            builder.append(hour + " : ");
        } else {
        	builder.append("00" + " : ");
        }

        if(minute < 10){
            builder.append("0");
        }
        builder.append(minute + " : ");
        if(sec < 10){
            builder.append("0");
        }
        builder.append(sec);
        return builder.toString();
    }

    public static String formatTime(int ms, int total) {
        if (ms < 0)
            return "";
        int sec = ms / 1000;
        int hour = sec / 3600;
        int minute = (sec % 3600) / 60;
        sec = sec % 3600 % 60;
        StringBuilder builder = new StringBuilder();
        int totalHour = (total / 1000) / 3600;
        if(totalHour > 0) {
            if (hour < 10) {
                builder.append(0);
            }
            builder.append(hour + ":");
        }

        if (minute < 10) {
            builder.append("0");
        }
        builder.append(minute + ":");
        if (sec < 10) {
            builder.append("0");
        }
        builder.append(sec);
        return builder.toString();
    }
    
    public static String getEncoding(String paramString) {
        String str = "";
        try {
            boolean bool4 = paramString.equals(new String(paramString
                    .getBytes("GB2312"), "GB2312"));
            if (bool4) {
                return "GB2312";
            }
            boolean bool3 = paramString.equals(new String(paramString
                    .getBytes("ISO-8859-1"), "ISO-8859-1"));
            if (bool3) {
                return "ISO-8859-1";
            }
            boolean bool2 = paramString.equals(new String(paramString
                    .getBytes("UTF-8"), "UTF-8"));
            if (bool2) {
                return "UTF-8";
            }
            boolean bool1 = paramString.equals(new String(
                    paramString.getBytes("GBK"), "GBK"));
            if (bool1) {
                return "GBK";
            }

        } catch (Exception localException1) {
        }
        return str;
    }
    
    /**
     * 转码utf-8
     * @param s
     * @return
     */
    public static String toUnicode(String s) {
        if(TextUtils.isEmpty(s)) {
            return "";
        }
        String str = "";
        try {
            str = URLDecoder.decode(s, "utf-8");
        } catch (UnsupportedEncodingException e) {
            throw new RuntimeException(e);
        }
        StringBuffer sb = new StringBuffer();
        for (int i = 0; i < str.length(); i++) {
            if (str.charAt(i) <= 256) {
                sb.append("%u00");
            } else {
                sb.append("%u");
            }
            sb.append(Integer.toHexString(str.charAt(i)));
        }
        return sb.toString();
    }
    
    /**
     * 处理
     * @param textView
     * @param textStr
     */
    public void handleTextView(TextView textView, String textStr, int maxLineCount) {
        textView.setText(textStr);//首先要赋值一次，让系统自动处理，产生自动换行
        ViewTreeObserver vto = textView.getViewTreeObserver();
        MyOnGlobalLayoutListener layoutListener = new MyOnGlobalLayoutListener(textView, textStr, maxLineCount);
        vto.addOnGlobalLayoutListener(layoutListener);
    }

    class MyOnGlobalLayoutListener implements OnGlobalLayoutListener
    {
        private TextView textView;
        private String textValue;
        private int maxLineCount;
    
        public MyOnGlobalLayoutListener(TextView textView, String textValue, int maxLineCount)
        {
            this.textView = textView;
            this.textValue = textValue;
            this.maxLineCount = maxLineCount;
        }
    
        @SuppressWarnings("deprecation")
        @Override
        public void onGlobalLayout()
        {
            ViewTreeObserver obs = textView.getViewTreeObserver();
            obs.removeGlobalOnLayoutListener(this);
            if (textView.getLineCount() >= maxLineCount)//如果一行显示不下而自动换行，所以要在前台文件作修改，去掉singleLine=true，否则该条件不会成立。
            {
                int lineEndIndex = this.textView.getLayout().getLineEnd(maxLineCount - 1);//获取被截断的字符长度
                String text = textValue.subSequence(0, lineEndIndex - 3) + "...";//手动加上省略号
                textView.setText(text);
            }
        }
    }

    public static String deleteChars(String text, String...string) {
        if(StringUtil.isEmptyStr(text)) {
            return "";
        }
        for(String s : string) {
            text = text.replace(s, "");
        }
        return text;
    }
}
