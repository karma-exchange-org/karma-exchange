package org.karmaexchange.resources.msg;

import java.util.List;

import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlSeeAlso;

import lombok.Data;

@XmlRootElement
@Data
@XmlSeeAlso({EventParticipantView.class})
public class ListResponseMsg<T> {

  private List<T> data;
  private PagingInfo paging;

  public static class PagingInfo {
    String next;
    String prev;
  }
}
