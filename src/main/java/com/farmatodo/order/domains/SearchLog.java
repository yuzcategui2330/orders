package com.farmatodo.order.domains;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "search_logs")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class SearchLog {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@JsonProperty("id")
	@Column(name = "id")
	private Long id;

	@JsonProperty("search_term")
	@Column(name = "search_term", nullable = false, length = 200)
	private String searchTerm;

	@JsonProperty("result_count")
	@Column(name = "result_count", nullable = false)
	private Long resultCount;

	@JsonProperty("created_at")
	@Column(name = "created_at", nullable = false)
	private LocalDateTime createdAt;

	public static SearchLog create(String searchTerm, long resultCount) {
		SearchLog searchLog = new SearchLog();
		searchLog.searchTerm = searchTerm;
		searchLog.resultCount = resultCount;
		return searchLog;
	}

	@PrePersist
	void onCreate() {
		this.createdAt = LocalDateTime.now();
	}
}
