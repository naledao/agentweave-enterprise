package com.agentweave.tool.log;

import java.util.regex.Pattern;
import org.springframework.stereotype.Service;

@Service
public class LogMaskingService {

    private static final Pattern KEY_VALUE_SECRET = Pattern.compile(
            "(?i)(api[-_ ]?key|token|secret|password|authorization)\\s*[=:]\\s*[^\\s,;]+");
    private static final Pattern BEARER_TOKEN = Pattern.compile("(?i)bearer\\s+[A-Za-z0-9._\\-]+");
    private static final Pattern MAINLAND_PHONE = Pattern.compile("(?<!\\d)1[3-9]\\d{9}(?!\\d)");
    private static final Pattern ID_CARD = Pattern.compile("(?<!\\d)\\d{6}(?:18|19|20)\\d{2}\\d{7}[0-9Xx](?!\\d)");

    public String mask(String value) {
        if (value == null || value.isBlank()) {
            return value;
        }
        String masked = KEY_VALUE_SECRET.matcher(value).replaceAll(match -> {
            String text = match.group();
            int separatorIndex = firstSeparatorIndex(text);
            if (separatorIndex < 0) {
                return "******";
            }
            return text.substring(0, separatorIndex + 1) + "******";
        });
        masked = BEARER_TOKEN.matcher(masked).replaceAll("Bearer ******");
        masked = MAINLAND_PHONE.matcher(masked).replaceAll(match -> {
            String phone = match.group();
            return phone.substring(0, 3) + "****" + phone.substring(7);
        });
        return ID_CARD.matcher(masked).replaceAll(match -> {
            String idCard = match.group();
            return idCard.substring(0, 6) + "********" + idCard.substring(14);
        });
    }

    private int firstSeparatorIndex(String value) {
        int equalsIndex = value.indexOf('=');
        int colonIndex = value.indexOf(':');
        if (equalsIndex < 0) {
            return colonIndex;
        }
        if (colonIndex < 0) {
            return equalsIndex;
        }
        return Math.min(equalsIndex, colonIndex);
    }
}
