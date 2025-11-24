package org.schabi.newpipe.extractor.services.youtube.linkHandler;

import static org.schabi.newpipe.extractor.utils.Utils.encodeUrlUtf8;

import org.schabi.newpipe.extractor.exceptions.ParsingException;
import org.schabi.newpipe.extractor.linkhandler.SearchQueryHandlerFactory;

import java.util.List;

import javax.annotation.Nonnull;

public final class YoutubeKidsSearchQueryHandlerFactory extends SearchQueryHandlerFactory {

    private static final YoutubeKidsSearchQueryHandlerFactory INSTANCE =
            new YoutubeKidsSearchQueryHandlerFactory();

    private static final String KIDS_SEARCH_URL =
            "https://www.youtubekids.com/results?search_query=";

    @Nonnull
    public static YoutubeKidsSearchQueryHandlerFactory getInstance() {
        return INSTANCE;
    }

    @Override
    public String getUrl(final String searchString,
                         @Nonnull final List<String> contentFilters,
                         final String sortFilter)
            throws ParsingException {
        return KIDS_SEARCH_URL + encodeUrlUtf8(searchString);
    }

    @Override
    public String[] getAvailableContentFilter() {
        // Kids 搜索不支持复杂 filter，同时保持结构一致
        return new String[] { "all" };
    }
}
