import { useEffect, useId, useRef, useState } from "react";
import {
  format,
  startOfMonth,
  endOfMonth,
  startOfWeek,
  endOfWeek,
  addDays,
  addMonths,
  subMonths,
  isSameMonth,
  isSameDay,
  isToday,
} from "date-fns";

interface DatePickerProps {
  value?: string;
  placeholder?: string;
  onChange?: (value: string) => void;
  className?: string;
  inputSize?: "sm" | "md";
  label?: string;
}

export default function DatePicker({
  value = "",
  placeholder = "날짜 선택",
  onChange,
  className = "",
  inputSize = "md",
  label,
}: DatePickerProps) {
  const [isOpen, setIsOpen] = useState(false);
  const [currentMonth, setCurrentMonth] = useState(new Date());
  const containerRef = useRef<HTMLDivElement>(null);
  const inputId = useId();

  const selectedDate = value ? new Date(value.replace(/\./g, "-")) : undefined;

  useEffect(() => {
    const handleClickOutside = (event: MouseEvent) => {
      if (
        containerRef.current &&
        !containerRef.current.contains(event.target as Node)
      ) {
        setIsOpen(false);
      }
    };

    if (isOpen) {
      document.addEventListener("mousedown", handleClickOutside);
    }

    return () => {
      document.removeEventListener("mousedown", handleClickOutside);
    };
  }, [isOpen]);

  const handleDateSelect = (date: Date) => {
    const formattedDate = format(date, "yyyy.MM.dd");
    onChange?.(formattedDate);
    setIsOpen(false);
  };

  const handlePrevMonth = () => {
    setCurrentMonth(subMonths(currentMonth, 1));
  };

  const handleNextMonth = () => {
    setCurrentMonth(addMonths(currentMonth, 1));
  };

  const renderCalendar = () => {
    const monthStart = startOfMonth(currentMonth);
    const monthEnd = endOfMonth(monthStart);
    const startDate = startOfWeek(monthStart);
    const endDate = endOfWeek(monthEnd);

    const rows = [];
    let days = [];
    let day = startDate;

    while (day <= endDate) {
      for (let i = 0; i < 7; i++) {
        const currentDay = day;
        const isCurrentMonth = isSameMonth(day, monthStart);
        const isSelected = selectedDate && isSameDay(day, selectedDate);
        const isTodayDate = isToday(day);

        days.push(
          <button
            key={day.toString()}
            type="button"
            onClick={() => handleDateSelect(currentDay)}
            className={`
              h-9 w-9 flex items-center justify-center rounded-lg text-14-m
              ${!isCurrentMonth ? "text-gray-400" : "text-gray-900"}
              ${isSelected ? "bg-cyan-500 text-white font-semibold" : ""}
              ${isTodayDate && !isSelected ? "border border-cyan-500" : ""}
              ${isCurrentMonth && !isSelected ? "hover:bg-gray-100" : ""}
            `}
          >
            {format(day, "d")}
          </button>,
        );

        day = addDays(day, 1);
      }

      rows.push(
        <div key={day.toString()} className="flex gap-1 justify-between">
          {days}
        </div>,
      );
      days = [];
    }

    return rows;
  };

  const sizeClasses = {
    sm: "min-h-10 py-1.5 px-3",
    md: "min-h-14 py-4 px-5",
  };

  return (
    <div className={className} ref={containerRef}>
      <button
        type="button"
        id={inputId}
        onClick={() => setIsOpen(!isOpen)}
        className={`w-full border rounded-lg mt-1.5 bg-white ${sizeClasses[inputSize]} border-gray-200 focus:border-cyan-500 outline-none text-left flex items-center gap-2`}
      >
        <span
          className={`text-16-m ${value ? "text-gray-900" : "text-gray-400"}`}
        >
          {value || placeholder}
        </span>
        {value && label && (
          <span className="text-14-m text-gray-500">{label}</span>
        )}
      </button>

      {isOpen && (
        <div className="absolute z-50 mt-2 bg-white border border-gray-200 rounded-lg shadow-lg p-4 w-[280px]">
          <div className="flex items-center justify-between mb-4">
            <button
              type="button"
              onClick={handlePrevMonth}
              className="p-1 hover:bg-gray-100 rounded"
            >
              <svg
                className="w-5 h-5"
                fill="none"
                stroke="currentColor"
                viewBox="0 0 24 24"
              >
                <path
                  strokeLinecap="round"
                  strokeLinejoin="round"
                  strokeWidth={2}
                  d="M15 19l-7-7 7-7"
                />
              </svg>
            </button>

            <div className="text-16-b text-gray-900">
              {format(currentMonth, "yyyy년 M월")}
            </div>

            <button
              type="button"
              onClick={handleNextMonth}
              className="p-1 hover:bg-gray-100 rounded"
            >
              <svg
                className="w-5 h-5"
                fill="none"
                stroke="currentColor"
                viewBox="0 0 24 24"
              >
                <path
                  strokeLinecap="round"
                  strokeLinejoin="round"
                  strokeWidth={2}
                  d="M9 5l7 7-7 7"
                />
              </svg>
            </button>
          </div>

          <div className="mb-2">
            <div className="flex gap-1 justify-between mb-2">
              {["일", "월", "화", "수", "목", "금", "토"].map((day) => (
                <div
                  key={day}
                  className="h-9 w-9 flex items-center justify-center text-14-m text-gray-600 font-semibold"
                >
                  {day}
                </div>
              ))}
            </div>
          </div>

          <div className="flex flex-col gap-1">{renderCalendar()}</div>
        </div>
      )}
    </div>
  );
}
