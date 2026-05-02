package com.madassignment.myaccomodationapp.presentation.home

import androidx.paging.PagingSource
import androidx.paging.PagingState
import com.madassignment.myaccomodationapp.domain.model.Listing
import com.madassignment.myaccomodationapp.domain.model.ListingFilters
import com.madassignment.myaccomodationapp.domain.usecase.FetchListingsPageUseCase

class ListingPagingSource(
    private val fetchListingsPage: FetchListingsPageUseCase,
    private val filters: ListingFilters,
) : PagingSource<String, Listing>() {

    override suspend fun load(params: LoadParams<String>): LoadResult<String, Listing> {
        val result = fetchListingsPage(filters, params.loadSize, params.key)
        return result.fold(
            onSuccess = { page ->
                LoadResult.Page(
                    data = page.items,
                    prevKey = null,
                    nextKey = page.nextCursor,
                )
            },
            onFailure = { LoadResult.Error(it) },
        )
    }

    override fun getRefreshKey(state: PagingState<String, Listing>): String? = null
}
