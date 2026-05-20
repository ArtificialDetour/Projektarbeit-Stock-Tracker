package com.project.stocktracker.dto;

/**
 * Search result returned by the asset lookup endpoint.
 */
public record AssetSearchResultDto(
    String symbol, 
    String name, 
    String exchange
    ) {}
