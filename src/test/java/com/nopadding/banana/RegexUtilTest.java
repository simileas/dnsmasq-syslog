package com.nopadding.banana;

import java.util.regex.Matcher;
import java.util.regex.Pattern;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;

@Slf4j
class RegexUtilTest {

  private static final Pattern RECORD_PATTERN = Pattern.compile("<([A-Z]*)>");

  @Test
  public void test() {
    Matcher matcher = RECORD_PATTERN.matcher("<TXT>");
    while (matcher.find()) {
      log.info("Group: {}", matcher.group(1));
      ;
    }
  }
}
