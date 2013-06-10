package org.karmaexchange.resources.msg;

import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import javax.xml.bind.annotation.XmlRootElement;

import org.karmaexchange.dao.Rating;
import org.karmaexchange.dao.Review;
import org.karmaexchange.dao.User;

import com.google.common.collect.Lists;
import com.googlecode.objectify.Key;

import lombok.Data;
import lombok.NoArgsConstructor;

@XmlRootElement
@Data
@NoArgsConstructor
public final class ReviewCommentView {

  private Rating rating;
  private Date commentCreationDate;
  private String comment;
  private EventParticipantView authorInfo;

  public static List<ReviewCommentView> create(List<Review> reviews) {
    removeReviewsWithoutComments(reviews);
    Map<Key<User>, EventParticipantView> authorInfo = loadAuthorInfo(reviews);
    List<ReviewCommentView> result = Lists.newArrayList();
    for (Review review : reviews) {
      result.add(new ReviewCommentView(review, authorInfo.get(review.getAuthor())));
    }
    return result;
  }

  private static void removeReviewsWithoutComments(List<Review> reviews) {
    Iterator<Review> reviewIter = reviews.iterator();
    while (reviewIter.hasNext()) {
      Review review = reviewIter.next();
      if (review.getComment() == null) {
        reviewIter.remove();
      }
    }
  }

  private static Map<Key<User>, EventParticipantView> loadAuthorInfo(List<Review> reviews) {
    List<Key<User>> reviewAuthors = Lists.newArrayList();
    for (Review review : reviews) {
      reviewAuthors.add(review.getAuthor());
    }
    return EventParticipantView.getMap(reviewAuthors);
  }

  private ReviewCommentView(Review review, EventParticipantView authorInfo) {
    rating = review.getRating();
    commentCreationDate = review.getCommentCreationDate();
    comment = review.getComment();
    this.authorInfo = authorInfo;
  }
}
