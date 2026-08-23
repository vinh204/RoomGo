package com.example.homestay.ui.adapter;

import com.example.homestay.data.entity.Review;

public final class ReviewDisplayItem {
  private final Review review;
  private final String reviewerName;

  public ReviewDisplayItem(Review review, String reviewerName) {
    this.review = review;
    this.reviewerName = reviewerName;
  }

  public Review getReview() {
    return review;
  }

  public String getReviewerName() {
    return reviewerName;
  }
}
