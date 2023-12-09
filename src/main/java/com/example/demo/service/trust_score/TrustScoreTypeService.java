package com.example.demo.service.trust_score;

import com.example.demo.dto.trust_score_type.TrustScoreTypeSearchCriteria;
import com.example.demo.dto.trust_score_type.request.TrustScoreTypeCreateRequestDto;
import com.example.demo.dto.trust_score_type.response.TrustScoreTypeCreateResponseDto;
import com.example.demo.dto.trust_score_type.response.TrustScoreTypeReadResponseDto;

import java.util.List;

public interface TrustScoreTypeService {
    List<TrustScoreTypeReadResponseDto> getAllAndReturnDto();
    List<TrustScoreTypeReadResponseDto> getSearchResults(
            TrustScoreTypeSearchCriteria criteria);
    TrustScoreTypeCreateResponseDto createTrustScoreType(TrustScoreTypeCreateRequestDto requestDto);
}
