package org.schabi.newpipe.extractor.services.youtube.extractors;

import static org.schabi.newpipe.extractor.services.youtube.YoutubeParsingHelper.getJsonPostResponse;
import static org.schabi.newpipe.extractor.services.youtube.YoutubeParsingHelper.prepareDesktopJsonBuilder;
import static org.schabi.newpipe.extractor.services.youtube.YoutubeParsingHelper.getTextFromObject;

import com.grack.nanojson.JsonBuilder;
import com.grack.nanojson.JsonObject;
import com.grack.nanojson.JsonWriter;

import org.schabi.newpipe.extractor.InfoItem;
import org.schabi.newpipe.extractor.MetaInfo;
import org.schabi.newpipe.extractor.Page;
import org.schabi.newpipe.extractor.StreamingService;
import org.schabi.newpipe.extractor.downloader.Downloader;
import org.schabi.newpipe.extractor.exceptions.ExtractionException;
import org.schabi.newpipe.extractor.exceptions.ParsingException;
import org.schabi.newpipe.extractor.linkhandler.SearchQueryHandler;
import org.schabi.newpipe.extractor.localization.TimeAgoParser;
import org.schabi.newpipe.extractor.search.SearchExtractor;
import org.schabi.newpipe.extractor.services.youtube.YoutubeMetaInfoHelper;
import org.schabi.newpipe.extractor.utils.JsonUtils;
import org.schabi.newpipe.extractor.MultiInfoItemsCollector;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

import java.util.List;
import java.util.Objects;

import javax.annotation.Nonnull;

public class YoutubeKidsSearchExtractor extends SearchExtractor {

        private final boolean extractVideoResults;
        private final boolean extractChannelResults;
        private JsonObject initialData;

        public YoutubeKidsSearchExtractor(final StreamingService service,
                        final SearchQueryHandler linkHandler) {
                super(service, linkHandler);
                // YouTube Kids doesn't support search filters (videos-only, channels-only,
                // etc.)
                // So we always extract both video and channel results
                extractVideoResults = true;
                extractChannelResults = true;
        }

        @Override
        public void onFetchPage(@Nonnull final Downloader downloader)
                        throws IOException, ExtractionException {

                final String query = super.getSearchString();

                // Build request JSON body based on the provided curl command
                final JsonBuilder<JsonObject> bodyBuilder = prepareDesktopJsonBuilder(
                                getExtractorLocalization(), getExtractorContentCountry())
                                .value("context", prepareDesktopJsonBuilder(
                                                getExtractorLocalization(), getExtractorContentCountry())
                                                .value("client", prepareDesktopJsonBuilder(
                                                                getExtractorLocalization(),
                                                                getExtractorContentCountry())
                                                                .value("clientName", "WEB_KIDS")
                                                                .value("clientVersion", "2.20251120.00.00")
                                                                .value("kidsAppInfo", prepareDesktopJsonBuilder(
                                                                                getExtractorLocalization(),
                                                                                getExtractorContentCountry())
                                                                                .value("contentSettings",
                                                                                                prepareDesktopJsonBuilder(
                                                                                                                getExtractorLocalization(),
                                                                                                                getExtractorContentCountry())
                                                                                                                .value("corpusPreference",
                                                                                                                                "KIDS_CORPUS_PREFERENCE_TWEEN")
                                                                                                                .value("kidsNoSearchMode",
                                                                                                                                "YT_KIDS_NO_SEARCH_MODE_OFF")
                                                                                                                .done())
                                                                                .done())
                                                                .done())
                                                .done())
                                .value("query", query);

                final byte[] postBody = JsonWriter.string(bodyBuilder.done())
                                .getBytes(StandardCharsets.UTF_8);

                initialData = getJsonPostResponse("search", postBody, getExtractorLocalization());
        }

        @Nonnull
        @Override
        public String getName() {
                return "YouTube Kids";
        }

        @Nonnull
        @Override
        public String getSearchSuggestion() throws ParsingException {
                final JsonObject itemSectionRenderer = initialData.getObject("contents")
                                .getObject("sectionListRenderer")
                                .getArray("contents")
                                .getObject(0)
                                .getObject("itemSectionRenderer");
                final JsonObject didYouMeanRenderer = itemSectionRenderer.getArray("contents")
                                .getObject(0)
                                .getObject("didYouMeanRenderer");

                if (!didYouMeanRenderer.isEmpty()) {
                        return JsonUtils.getString(didYouMeanRenderer,
                                        "correctedQueryEndpoint.searchEndpoint.query");
                }

                return Objects.requireNonNullElse(
                                getTextFromObject(itemSectionRenderer.getArray("contents")
                                                .getObject(0)
                                                .getObject("showingResultsForRenderer")
                                                .getObject("correctedQuery")),
                                "");
        }

        @Override
        public boolean isCorrectedSearch() throws ParsingException {
                final JsonObject showingResultsForRenderer = initialData.getObject("contents")
                                .getObject("sectionListRenderer").getArray("contents").getObject(0)
                                .getObject("itemSectionRenderer").getArray("contents").getObject(0)
                                .getObject("showingResultsForRenderer");
                return !showingResultsForRenderer.isEmpty();
        }

        @Nonnull
        @Override
        public List<MetaInfo> getMetaInfo() throws ParsingException {
                return YoutubeMetaInfoHelper.getMetaInfo(
                                initialData.getObject("contents")
                                                .getObject("sectionListRenderer")
                                                .getArray("contents"));
        }

        @Nonnull
        @Override
        public InfoItemsPage<InfoItem> getInitialPage() throws IOException, ExtractionException {
                final MultiInfoItemsCollector collector = new MultiInfoItemsCollector(getServiceId());
                final TimeAgoParser timeAgoParser = getTimeAgoParser();

                if (initialData != null && initialData.has("contents")) {
                        JsonObject contents = initialData.getObject("contents");
                        if (contents.has("sectionListRenderer")) {

                                // ① 先收集频道
                                contents.getObject("sectionListRenderer")
                                        .getArray("contents").stream()
                                        .filter(JsonObject.class::isInstance)
                                        .map(JsonObject.class::cast)
                                        .filter(obj -> obj.has("itemSectionRenderer"))
                                        .map(obj -> obj.getObject("itemSectionRenderer"))
                                        .flatMap(section -> section.getArray("contents").stream())
                                        .filter(JsonObject.class::isInstance)
                                        .map(JsonObject.class::cast)
                                        .filter(content -> content.has("compactChannelRenderer"))
                                        .forEachOrdered(content -> {
                                                collector.commit(new YoutubeChannelInfoItemExtractor(
                                                        content.getObject("compactChannelRenderer")));
                                        });

                                // ② 再收集视频
                                contents.getObject("sectionListRenderer")
                                        .getArray("contents").stream()
                                        .filter(JsonObject.class::isInstance)
                                        .map(JsonObject.class::cast)
                                        .filter(obj -> obj.has("itemSectionRenderer"))
                                        .map(obj -> obj.getObject("itemSectionRenderer"))
                                        .flatMap(section -> section.getArray("contents").stream())
                                        .filter(JsonObject.class::isInstance)
                                        .map(JsonObject.class::cast)
                                        .filter(content -> content.has("compactVideoRenderer"))
                                        .forEachOrdered(content -> {
                                                collector.commit(new YoutubeStreamInfoItemExtractor(
                                                        content.getObject("compactVideoRenderer"),
                                                        timeAgoParser));
                                        });
                        }
                }

                return new InfoItemsPage<>(collector, null);
        }


        @Override
        public InfoItemsPage<InfoItem> getPage(final Page page) throws IOException, ExtractionException {
                return InfoItemsPage.emptyPage();
        }
}
