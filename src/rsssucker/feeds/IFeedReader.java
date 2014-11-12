/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package rsssucker.feeds;

import java.util.List;

/**
 *
 * @author damir
 */
public interface IFeedReader {
    
    public List<FeedEntry> getFeedEntries(String feedUrl) throws Exception;
    
}
