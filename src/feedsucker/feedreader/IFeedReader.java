package feedsucker.feedreader;

import java.util.List;

public interface IFeedReader {
    
    public List<FeedEntry> getFeedEntries(String feedUrl) throws Exception;
    
}
