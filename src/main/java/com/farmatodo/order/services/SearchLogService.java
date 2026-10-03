package com.farmatodo.order.services;

import com.farmatodo.order.config.AsyncConfig;
import com.farmatodo.order.domains.SearchLog;
import com.farmatodo.order.repositories.SearchLogRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class SearchLogService {

	private static final int MAX_TERM_LENGTH = 200;

	private final SearchLogRepository searchLogRepository;

	public SearchLogService(SearchLogRepository searchLogRepository) {
		this.searchLogRepository = searchLogRepository;
	}

	@Async(AsyncConfig.SEARCH_LOG_EXECUTOR)
	public void record(String searchTerm, long resultCount) {
		try {
			searchLogRepository.save(SearchLog.create(trimTerm(searchTerm), resultCount));
		} catch (RuntimeException exception) {
			log.warn("Search log was not stored for term {}", searchTerm, exception);
		}
	}

	private String trimTerm(String searchTerm) {
		if (searchTerm.length() <= MAX_TERM_LENGTH) {
			return searchTerm;
		}
		return searchTerm.substring(0, MAX_TERM_LENGTH);
	}
}
