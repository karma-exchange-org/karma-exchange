package org.karmaexchange.dao;

import java.util.Comparator;
import java.util.Date;

import javax.annotation.Nullable;

import lombok.Data;
import lombok.NoArgsConstructor;

import com.google.common.base.Predicate;
import com.googlecode.objectify.Key;

@Data
@NoArgsConstructor
public class AttendanceRecord {
  private KeyWrapper<Event> event;
  private Date eventStartTime;
  private boolean attended;

  public AttendanceRecord(Event event, boolean attended) {
    this.event = KeyWrapper.create(event);
    eventStartTime = event.getStartTime();
    this.attended = attended;
  }

  public static class EventStartTimeComparator implements Comparator<AttendanceRecord> {
    public static final EventStartTimeComparator INSTANCE = new EventStartTimeComparator();

    @Override
    public int compare(AttendanceRecord rec1, AttendanceRecord rec2) {
      // Recent events take precedence.
      return rec2.eventStartTime.compareTo(rec1.eventStartTime);
    }
  }

  public static Predicate<AttendanceRecord> eventPredicate(final Key<Event> eventKey) {
    return new Predicate<AttendanceRecord>() {
      @Override
      public boolean apply(@Nullable AttendanceRecord input) {
        return KeyWrapper.toKey(input.event).equals(eventKey);
      }
    };
  }
}
