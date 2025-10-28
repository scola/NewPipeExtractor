package org.schabi.newpipe.extractor.services.youtube.linkHandler;

import static org.schabi.newpipe.extractor.services.youtube.YoutubeParsingHelper.isInvidiousURL;
import static org.schabi.newpipe.extractor.services.youtube.YoutubeParsingHelper.isYoutubeURL;

import org.schabi.newpipe.extractor.exceptions.ParsingException;
import org.schabi.newpipe.extractor.linkhandler.ListLinkHandlerFactory;
import org.schabi.newpipe.extractor.utils.Utils;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.List;

public final class YoutubeKidsRecommendedLinkHandlerFactory extends ListLinkHandlerFactory {

    public static final String KIOSK_ID = "live";

    public static final YoutubeKidsRecommendedLinkHandlerFactory INSTANCE =
            new YoutubeKidsRecommendedLinkHandlerFactory();

    private static final String KIDS_HOME_PATH = "/youtubei/v1/browse";
    private static final String BASE_URL = "https://www.youtubekids.com";

    private YoutubeKidsRecommendedLinkHandlerFactory() {
    }

    @Override
    public String getUrl(final String id,
                         final List<String> contentFilters,
                         final String sortFilter)
            throws ParsingException, UnsupportedOperationException {
        return BASE_URL + KIDS_HOME_PATH + "?alt=json";
    }

    @Override
    public String getId(final String url) throws ParsingException, UnsupportedOperationException {
        return KIOSK_ID;
    }

    @Override
    public boolean onAcceptUrl(final String url) {
        final URL urlObj;
        try {
            urlObj = Utils.stringToURL(url);
        } catch (final MalformedURLException e) {
            return false;
        }

        return Utils.isHTTP(urlObj)
                && (urlObj.getHost().contains("youtubekids.com")
                || isYoutubeURL(urlObj) || isInvidiousURL(urlObj))
                && urlObj.getPath().contains("/youtubei/v1/browse");
    }
}
