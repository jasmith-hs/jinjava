package com.hubspot.jinjava.objects.date;

import com.hubspot.jinjava.objects.PyWrapper;
import java.io.Serializable;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoField;
import java.util.Date;
import java.util.Objects;
import java.util.Optional;
import org.apache.commons.lang3.math.NumberUtils;

/**
 * an object which quacks like a python date
 *
 * @author jstehler
 *
 */
public final class PyishDate extends Date implements Serializable, PyWrapper {
  private static final long serialVersionUID = 1L;
  public static final String PYISH_DATE_FORMAT = "yyyy-MM-dd HH:mm:ss";

  private final ZonedDateTime date;

  public PyishDate(ZonedDateTime dt) {
    super(dt.toInstant().toEpochMilli());
    this.date = dt;
  }

  public PyishDate(Date d) {
    this(ZonedDateTime.ofInstant(d.toInstant(), ZoneOffset.UTC));
  }

  public PyishDate(String publishDateStr) {
    this(NumberUtils.toLong(Objects.requireNonNull(publishDateStr), 0L));
  }

  public PyishDate(Long epochMillis) {
    this(
      ZonedDateTime.ofInstant(
        Instant.ofEpochMilli(
          Optional.ofNullable(epochMillis).orElseGet(System::currentTimeMillis)
        ),
        ZoneOffset.UTC
      )
    );
  }

  public PyishDate(Instant instant) {
    this(ZonedDateTime.ofInstant(instant, ZoneOffset.UTC));
  }

  public String isoformat() {
    return strftime("yyyy-MM-dd");
  }

  public String strftime(String fmt) {
    return StrftimeFormatter.format(date, fmt);
  }

  @SuppressWarnings("deprecation")
  @Override
  public int getYear() {
    return date.getYear();
  }

  @SuppressWarnings("deprecation")
  @Override
  public int getMonth() {
    return date.getMonthValue();
  }

  @SuppressWarnings("deprecation")
  @Override
  public int getDay() {
    return date.getDayOfMonth();
  }

  public int getHour() {
    return date.getHour();
  }

  public int getMinute() {
    return date.getMinute();
  }

  public int getSecond() {
    return date.getSecond();
  }

  public int getMicrosecond() {
    return date.get(ChronoField.MILLI_OF_SECOND);
  }

  public Date toDate() {
    return Date.from(date.toInstant());
  }

  public ZonedDateTime toDateTime() {
    return date;
  }

  @Override
  public String toString() {
    return strftime(PYISH_DATE_FORMAT);
  }

  @Override
  public int hashCode() {
    return Objects.hashCode(date);
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }
    if (obj == null || getClass() != obj.getClass()) {
      return false;
    }
    PyishDate that = (PyishDate) obj;
    return Objects.equals(toDateTime(), that.toDateTime());
  }
}
