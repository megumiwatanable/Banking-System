package com.banking.credit.application;

import com.banking.account.domain.Account;
import com.banking.account.infrastructure.AccountRepository;
import com.banking.asset.domain.CustomerAsset;
import com.banking.asset.infrastructure.CustomerAssetRepository;
import com.banking.credit.api.dto.CreditRatingResponse;
import com.banking.credit.domain.CreditRating;
import com.banking.credit.infrastructure.CreditRatingRepository;
import com.banking.shared.error.ResourceNotFoundException;
import com.banking.shared.security.SecurityUtils;
import com.banking.transaction.domain.TransactionStatus;
import com.banking.transaction.infrastructure.TransactionRepository;
import com.banking.user.domain.User;
import com.banking.user.infrastructure.UserRepository;
import java.math.BigDecimal;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CreditRatingService {
  private static final int BASE_SCORE = 500;
  private static final int MINIMUM_SCORE = 300;
  private static final int MAXIMUM_SCORE = 850;
  private static final BigDecimal BALANCE_SCORE_UNIT = new BigDecimal("1000000");
  private static final BigDecimal ASSET_SCORE_UNIT = new BigDecimal("10000000");
  private static final String DISCLAIMER =
      "Internal indicative rating based only on data in this application; it is not a CIC score or"
          + " a lending decision.";
  private final CreditRatingRepository creditRatingRepository;
  private final AccountRepository accountRepository;
  private final CustomerAssetRepository assetRepository;
  private final TransactionRepository transactionRepository;
  private final UserRepository userRepository;

  public CreditRatingService(
      CreditRatingRepository creditRatingRepository,
      AccountRepository accountRepository,
      CustomerAssetRepository assetRepository,
      TransactionRepository transactionRepository,
      UserRepository userRepository) {
    this.creditRatingRepository = creditRatingRepository;
    this.accountRepository = accountRepository;
    this.assetRepository = assetRepository;
    this.transactionRepository = transactionRepository;
    this.userRepository = userRepository;
  }

  public CreditRatingResponse current() {
    return creditRatingRepository
        .findByUserEmail(SecurityUtils.getCurrentUserEmail())
        .map(this::toResponse)
        .orElseThrow(() -> new ResourceNotFoundException("Credit rating has not been evaluated"));
  }

  @Transactional
  public CreditRatingResponse evaluate() {
    String email = SecurityUtils.getCurrentUserEmail();
    User user =
        userRepository
            .findByEmail(email)
            .orElseThrow(() -> new ResourceNotFoundException("User not found"));
    BigDecimal balance =
        accountRepository.findByUserEmailOrderByIdDesc(email).stream()
            .map(Account::getBalance)
            .reduce(BigDecimal.ZERO, BigDecimal::add);
    BigDecimal assetValue =
        assetRepository.findByUserEmailOrderByUpdatedAtDesc(email).stream()
            .map(CustomerAsset::getEstimatedValue)
            .reduce(BigDecimal.ZERO, BigDecimal::add);
    long successful =
        transactionRepository
            .findBySenderAccountUserEmailOrReceiverAccountUserEmail(email, email)
            .stream()
            .filter(t -> t.getStatus() == TransactionStatus.SUCCESS)
            .count();
    int score =
        BASE_SCORE
            + Math.min(150, balance.divideToIntegralValue(BALANCE_SCORE_UNIT).intValue())
            + Math.min(100, assetValue.divideToIntegralValue(ASSET_SCORE_UNIT).intValue())
            + (int) Math.min(100, successful * 2);
    score = Math.max(MINIMUM_SCORE, Math.min(MAXIMUM_SCORE, score));
    String grade = score >= 750 ? "A" : score >= 650 ? "B" : score >= 550 ? "C" : "D";
    String explanation =
        "Balance, self-declared assets and successful internal transactions; balance="
            + balance
            + ", assets="
            + assetValue
            + ", successfulTransactions="
            + successful;
    CreditRating rating =
        creditRatingRepository.findByUserEmail(email).orElseGet(CreditRating::new);
    rating.setUser(user);
    rating.setScore(score);
    rating.setGrade(grade);
    rating.setExplanation(explanation);
    return toResponse(creditRatingRepository.save(rating));
  }

  private CreditRatingResponse toResponse(CreditRating rating) {
    return new CreditRatingResponse(
        rating.getScore(),
        rating.getGrade(),
        rating.getExplanation(),
        rating.getEvaluatedAt(),
        DISCLAIMER);
  }
}
