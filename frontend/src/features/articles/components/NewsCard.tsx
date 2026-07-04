import Label from "@/shared/components/Label";
import commentIcon from "@/assets/icons/comment.svg";
import { format } from "date-fns";
import type { ArticleListItem } from "@/api/articles/types";

interface NewsCardProps {
  article: ArticleListItem;
  onClick: () => void;
}

export default function NewsCard({ article, onClick }: NewsCardProps) {
  const formattedDate = format(article.publishDate, "yyyy.MM.dd");

  return (
    <>
      <div
        className="max-w-4xl w-auto min-h-48 h-auto cursor-pointer"
        onClick={onClick}
      >
        <div className="my-6 mx-1">
          <div className="text-20-b text-gray-900 mb-2">
            <span dangerouslySetInnerHTML={{ __html: article.title }} />
          </div>
          <div className="text-18-r text-gray-500 mb-6">
            <span dangerouslySetInnerHTML={{ __html: article.summary }} />
          </div>
          <div className="flex justify-between items-center">
            <Label label={article.source} />
            <div className="flex items-center gap-3">
              <span className="text-14-r text-gray-400">{formattedDate}</span>
              <span className="text-gray-300">|</span>
              <div className="flex items-center gap-1">
                <span className="text-14-r text-gray-400">읽음</span>
                <span className="text-14-r text-gray-400">
                  {article.viewCount}
                </span>
              </div>
              <span className="text-gray-300">|</span>
              <div className="flex items-center gap-1">
                <img src={commentIcon} className="w-5 h-5" alt="댓글" />
                <span className="text-14-r text-gray-400">
                  {article.commentCount}
                </span>
              </div>
            </div>
          </div>
        </div>
      </div>
    </>
  );
}
