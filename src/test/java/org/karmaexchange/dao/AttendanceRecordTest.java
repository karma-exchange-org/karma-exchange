package org.karmaexchange.dao;

import static org.junit.Assert.*;

import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.List;

import org.junit.Test;
import org.karmaexchange.dao.AttendanceRecord.EventStartTimeComparator;

public class AttendanceRecordTest {

  @Test
  public void testEventStartTimeComparator() {
    AttendanceRecord r1 = new AttendanceRecord();
    r1.setEventStartTime(new Date(1475833867382l));
    AttendanceRecord r2 = new AttendanceRecord();
    r2.setEventStartTime(new Date(1375833867382l));
    AttendanceRecord r3 = new AttendanceRecord();
    r3.setEventStartTime(new Date(1275833867382l));

    List<AttendanceRecord> l1 = Arrays.asList(r2, r1, r3);
    Collections.sort(l1, EventStartTimeComparator.INSTANCE);
    assertEquals(r1, l1.get(0));
  }

}
