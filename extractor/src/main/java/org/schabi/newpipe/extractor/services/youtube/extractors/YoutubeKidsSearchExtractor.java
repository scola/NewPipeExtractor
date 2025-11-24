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
                extractVideoResults = true;
                extractChannelResults = true;
        }

        @Override
        public void onFetchPage(@Nonnull final Downloader downloader)
                throws IOException, ExtractionException {

                final String query = super.getSearchString();

                final JsonBuilder<JsonObject> clientBuilder =
                        prepareDesktopJsonBuilder(getExtractorLocalization(), getExtractorContentCountry())
                                .value("clientName", "WEB_KIDS")
                                .value("clientVersion", "2.20251120.00.00")
                                .value("hl", "zh-CN")
                                .value("gl", "US")
                                .value("browserName", "Chrome")
                                .value("browserVersion", "142.0.0.0")
                                .value("osName", "Macintosh")
                                .value("osVersion", "10_15_7")
                                .value("platform", "DESKTOP")
                                .value("rolloutToken", "COGg5tzb_9OIwAEQqv3UpNe-kAMYvNDB0omKkQM%3D")
                                .value("kidsAppInfo",
                                        prepareDesktopJsonBuilder(getExtractorLocalization(), getExtractorContentCountry())
                                                .value("contentSettings",
                                                        prepareDesktopJsonBuilder(getExtractorLocalization(), getExtractorContentCountry())
                                                                .value("corpusPreference", "KIDS_CORPUS_PREFERENCE_TWEEN")
                                                                .value("kidsNoSearchMode", "YT_KIDS_NO_SEARCH_MODE_OFF")
                                                                .done()
                                                )
                                                .value("categorySettings",
                                                        prepareDesktopJsonBuilder(getExtractorLocalization(), getExtractorContentCountry())
                                                                .array("enabledCategories")
                                                                .value("approved_for_you")
                                                                .value("black_joy")
                                                                .value("camp")
                                                                .value("collections")
                                                                .value("earth")
                                                                .value("explore")
                                                                .value("favorites")
                                                                .value("gaming")
                                                                .value("halloween")
                                                                .value("hero")
                                                                .value("learning")
                                                                .value("making")
                                                                .value("move")
                                                                .value("music")
                                                                .value("reading")
                                                                .value("shared_by_parents")
                                                                .value("shows")
                                                                .value("soccer")
                                                                .value("sports")
                                                                .value("spotlight")
                                                                .value("winter")
                                                                .end()
                                                                .done()
                                                )
                                                .done()
                                );

                final JsonBuilder<JsonObject> bodyBuilder =
                        prepareDesktopJsonBuilder(getExtractorLocalization(), getExtractorContentCountry())
                                .value("context",
                                        prepareDesktopJsonBuilder(getExtractorLocalization(), getExtractorContentCountry())
                                                .value("client", clientBuilder.done())
                                                .done()
                                )
                                .value("query", query);

                final byte[] postBody =
                        JsonWriter.string(bodyBuilder.done()).getBytes(StandardCharsets.UTF_8);

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
