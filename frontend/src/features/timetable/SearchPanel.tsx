import { useState } from "react";
import { Search, X } from "lucide-react";
import { Input } from "@/components/common/Input";
import { Button } from "@/components/common/Button";
import { timetableApi } from "./api";
import type { TimetableEntry } from "./types";

interface Props {
  onResults: (entries: TimetableEntry[]) => void;
  onClear: () => void;
}

export function SearchPanel({ onResults, onClear }: Props) {
  const [query, setQuery] = useState("");
  const [searching, setSearching] = useState(false);

  const search = async () => {
    if (!query.trim()) { onClear(); return; }
    setSearching(true);
    try {
      const results = await timetableApi.search({ query: query.trim() });
      onResults(results.filter((e: TimetableEntry) => e.dayOfWeek));
    } finally {
      setSearching(false);
    }
  };

  const clear = () => {
    setQuery("");
    onClear();
  };

  return (
    <div className="flex items-end gap-2">
      <Input
        label=""
        placeholder="Search subject, teacher, or room..."
        value={query}
        onChange={(e: React.ChangeEvent<HTMLInputElement>) => setQuery(e.target.value)}
        onKeyDown={(e: React.KeyboardEvent<HTMLInputElement>) => e.key === "Enter" && search()}
        icon={<Search className="h-4 w-4" />}
        className="h-9 max-w-xs"
      />
      <Button className="h-8 px-3 text-xs" onClick={search} loading={searching}>
        Search
      </Button>
      {query && (
        <Button className="h-8 px-3 text-xs" variant="ghost" onClick={clear}>
          <X className="h-3.5 w-3.5" />
        </Button>
      )}
    </div>
  );
}
