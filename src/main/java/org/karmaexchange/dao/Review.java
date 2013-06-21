package org.karmaexchange.dao;

import static org.karmaexchange.util.UserService.getCurrentUserKey;

import java.util.Date;

import javax.annotation.Nullable;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlTransient;

import org.karmaexchange.resources.msg.ErrorResponseMsg;
import org.karmaexchange.resources.msg.ErrorResponseMsg.ErrorInfo;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;

import com.googlecode.objectify.Key;
import com.googlecode.objectify.annotation.Entity;
import com.googlecode.objectify.annotation.Index;

@XmlRootElement
@Entity
@Data
@NoArgsConstructor
@EqualsAndHashCode(callSuper=true)
@ToString(callSuper=true)
public class Review extends NameBaseDao<Review> {

  private Rating rating;
  @Index
  private Date commentCreationDate;
  private String comment;

  // At this point reviews will have no comments associated with them. Instead comments will
  // be associate with events and organization comments on facebook.

  public void initPreUpsert(Key<?> owner, Key<User> authorKey) {
    if (name == null) {
      name = authorKey.getString();
      this.owner = owner;
    }
  }

  @Override
  protected void preProcessInsert() {
    super.preProcessInsert();
    updateCommentCreationDate(null);
    validateReview();
  }

  @Override
  protected void processUpdate(Review prevReview) {
    super.processUpdate(prevReview);
    updateCommentCreationDate(prevReview);
    validateReview();
  }

  @Override
  protected void processDelete() {
    super.processDelete();
    validateAuthorMatches();
  }

  private void updateCommentCreationDate(Review prevReview) {
    if (comment == null) {
      commentCreationDate = null;
    } else {
      if ((prevReview != null) && (prevReview.commentCreationDate != null)) {
        // The user is just updating the comment so keep the previous comment creation date.
        commentCreationDate = prevReview.commentCreationDate;
      } else {
        commentCreationDate = new Date();
      }
    }
  }

  private void validateReview() {
    if (rating == null) {
      throw ErrorResponseMsg.createException(
        "a rating must be specified", ErrorInfo.Type.BAD_REQUEST);
    }
    validateAuthorMatches();
  }

  private void validateAuthorMatches() {
    if (!getCurrentUserKey().equals(getAuthor())) {
      throw ErrorResponseMsg.createException("reviews can only be mutated by the owner",
        ErrorInfo.Type.BAD_REQUEST);
    }
  }

  @XmlTransient
  @Nullable
  public Key<User> getAuthor() {
    return (name == null) ? null : Key.<User>create(name);
  }

  @Override
  protected Permission evalPermission() {
    // TODO(avaliani): fill this in. Organizers of events should have
    // the ability to delete pictures also if the picture is owned by an
    // event.
    if (getAuthor().equals(getCurrentUserKey())) {
      return Permission.ALL;
    } else {
      return Permission.READ;
    }
  }

  public static <T> Key<Review> getKeyForCurrentUser(T resource) {
    return getKeyForCurrentUser(Key.create(resource));
  }

  public static Key<Review> getKeyForCurrentUser(Key<?> owner) {
    return getKeyForUser(owner, getCurrentUserKey());
  }

  public static Key<Review> getKeyForUser(Key<?> owner, Key<User> authorKey) {
    return Key.<Review>create(owner, Review.class, authorKey.getString());
  }
}
