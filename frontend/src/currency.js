export function formatINR(value) {
  if (value === null || value === undefined) return "";
  const num = Number(value);
  if (Number.isNaN(num)) return "";
  return new Intl.NumberFormat('en-IN', { style: 'currency', currency: 'INR' }).format(num);
}
