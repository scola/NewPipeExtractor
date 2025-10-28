package org.schabi.newpipe.extractor.services.youtube.extractors.kiosk;

import static org.schabi.newpipe.extractor.services.youtube.YoutubeParsingHelper.getJsonPostResponse;
import static org.schabi.newpipe.extractor.services.youtube.YoutubeParsingHelper.prepareDesktopJsonBuilder;
import static org.schabi.newpipe.extractor.utils.Utils.isNullOrEmpty;

import com.grack.nanojson.JsonObject;
import com.grack.nanojson.JsonWriter;

import org.schabi.newpipe.extractor.Page;
import org.schabi.newpipe.extractor.StreamingService;
import org.schabi.newpipe.extractor.downloader.Downloader;
import org.schabi.newpipe.extractor.exceptions.ExtractionException;
import org.schabi.newpipe.extractor.exceptions.ParsingException;
import org.schabi.newpipe.extractor.kiosk.KioskExtractor;
import org.schabi.newpipe.extractor.linkhandler.ListLinkHandler;
import org.schabi.newpipe.extractor.localization.TimeAgoParser;
import org.schabi.newpipe.extractor.services.youtube.extractors.YoutubeStreamInfoItemExtractor;
import org.schabi.newpipe.extractor.stream.StreamInfoItem;
import org.schabi.newpipe.extractor.stream.StreamInfoItemsCollector;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

import javax.annotation.Nonnull;

public class YoutubeKidsRecommendedExtractor extends KioskExtractor<StreamInfoItem> {

    public static final String KIOSK_ID = "live";

    private JsonObject initialData;

    public YoutubeKidsRecommendedExtractor(final StreamingService service,
                                           final ListLinkHandler linkHandler,
                                           final String kioskId) {
        super(service, linkHandler, kioskId);
    }

    @Override
    public void onFetchPage(@Nonnull final Downloader downloader)
            throws IOException, ExtractionException {

        // 构建请求 JSON body
        final JsonObject bodyJson = prepareDesktopJsonBuilder(getExtractorLocalization(),
                getExtractorContentCountry())
                .value("context",
                        prepareDesktopJsonBuilder(getExtractorLocalization(),
                                getExtractorContentCountry())
                                .value("client",
                                        prepareDesktopJsonBuilder(getExtractorLocalization(),
                                                getExtractorContentCountry())
                                                .value("clientName", "WEB_KIDS")
                                                .value("clientVersion", "2.20251027.00.00")
                                                .done())
                                .done())
                .value("browseId", "FEkids_home")
                .done();

        final byte[] postBody = JsonWriter.string(bodyJson)
                .getBytes(StandardCharsets.UTF_8);

        initialData = getJsonPostResponse("browse", postBody, getExtractorLocalization());
//        System.out.println("YouTube Kids Response: " + initialData.toString());
    }

    @Nonnull
    @Override
    public String getName() throws ParsingException {
        return "YouTube Kids";
    }

    @Nonnull
    @Override
    public InfoItemsPage<StreamInfoItem> getInitialPage() throws ParsingException {
        final StreamInfoItemsCollector collector = new StreamInfoItemsCollector(getServiceId());
        final TimeAgoParser timeAgoParser = getTimeAgoParser();

        JsonObject kidsHome = initialData.getObject("contents")
                .getObject("kidsHomeScreenRenderer");

        kidsHome.getArray("anchors").stream()
                .filter(JsonObject.class::isInstance)
                .map(JsonObject.class::cast)
                .map(anchor -> anchor.getObject("anchoredSectionRenderer"))
                .map(section -> section.getObject("content").getObject("sectionListRenderer"))
                .flatMap(sectionList -> sectionList.getArray("contents").stream())
                .filter(JsonObject.class::isInstance)
                .map(JsonObject.class::cast)
                .map(itemSection -> itemSection.getObject("itemSectionRenderer").getArray("contents"))
                .flatMap(contents -> contents.stream())
                .filter(JsonObject.class::isInstance)
                .map(JsonObject.class::cast)
                .filter(content -> content.has("compactVideoRenderer"))
                .map(content -> content.getObject("compactVideoRenderer"))
                .forEachOrdered(videoRenderer -> collector.commit(
                        new YoutubeStreamInfoItemExtractor(videoRenderer, timeAgoParser)
                ));

        return new InfoItemsPage<>(collector, null);
    }

    @Override
    public InfoItemsPage<StreamInfoItem> getPage(final Page page) {
        return InfoItemsPage.emptyPage();
    }
}
