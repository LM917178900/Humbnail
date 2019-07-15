package cn.cncommdata.file.util;

import java.text.SimpleDateFormat;
import java.util.Calendar;

public class DateUtils {

    public static String timestamp2Date(String str_num) {

        //yy-MM-dd hh:mm:ss是12小时制格式.
        // yy-MM-dd HH:mm:ss是24小时制格式。
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        String date = null;
        if (str_num.length() == 13) {
            date = sdf.format(Long.parseLong(str_num));
        } else if (str_num.length() == 10) {
            date = sdf.format(Long.parseLong(str_num) * 1000);
        } else {
            return str_num;
        }
        return date;
    }

    /**
     * 获取当前timeMillis
     *
     * @return 当前时间戳
     */
    public static Long getCurrentTimeMill() {
        Calendar calendar = Calendar.getInstance();
        return calendar.getTimeInMillis();
    }
}