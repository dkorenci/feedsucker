package rsssucker.article.download;

interface IHtmlDownloader {

    String downloadHtml(String url) throws Exception;
    
}
