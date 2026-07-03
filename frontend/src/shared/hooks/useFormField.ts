import { useState, useCallback } from "react";

type Validator = (v: string) => string;

export function useFormField(initial = "", validate?: Validator) {
  const [value, setValue] = useState(initial);
  const [touched, setTouched] = useState(false);
  const [error, setError] = useState("");

  const runValidate = useCallback(
    (next: string) => {
      if (!validate) return "";
      const error = validate(next);
      setError(error);
      return error;
    },
    [validate],
  );

  const onChange = useCallback(
    (e: React.ChangeEvent<HTMLInputElement>) => {
      const next = e.target.value;
      setValue(next);
      if (touched) runValidate(next);
    },
    [touched, runValidate],
  );

  const onBlur = useCallback(() => {
    if (!touched) setTouched(true);
    runValidate(value);
  }, [touched, value, runValidate]);

  return {
    value,
    setValue,
    touched,
    error,
    setError,
    onChange,
    onBlur,
    validateNow: () => runValidate(value),
  };
}
