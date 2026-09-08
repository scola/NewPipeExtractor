import org.schabi.newpipe.downloader.DownloaderTestImpl;
import org.schabi.newpipe.extractor.NewPipe;
import org.schabi.newpipe.extractor.ServiceList;
import org.schabi.newpipe.extractor.channel.ChannelInfo;
import org.schabi.newpipe.extractor.playlist.PlaylistInfoItem;
import org.schabi.newpipe.extractor.search.SearchInfo;

public class PeppaJson {
    public static void main(String[] args) throws Exception {
        NewPipe.init(DownloaderTestImpl.getInstance());
        SearchInfo searchInfo = SearchInfo.getInfo(ServiceList.YouTube, ServiceList.YouTube.getSearchQHFactory().fromQuery("Peppa pig official channel"));
        String channelUrl = searchInfo.getRelatedItems().stream()
                .filter(item -> item.getInfoType() == org.schabi.newpipe.extractor.InfoItem.InfoType.CHANNEL)
                .findFirst()
                .get()
                .getUrl();
        System.out.println("Channel URL: " + channelUrl);

        ChannelInfo channelInfo = ChannelInfo.getInfo(ServiceList.YouTube, channelUrl);
        String pUrl = null;
        for (var tab : channelInfo.getTabs()) {
            var items = tab.getItems().getItems();
            for (var item : items) {
                if (item instanceof PlaylistInfoItem) {
                    pUrl = item.getUrl();
                    break;
                }
            }
            if (pUrl != null) break;
        }
        System.out.println("Playlist: " + pUrl);
    }
}
