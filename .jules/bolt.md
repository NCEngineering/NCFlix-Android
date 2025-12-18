## 2024-05-23 - RecyclerView Adapter Listener Optimization
**Learning:** Moving `setOnClickListener` from `onBindViewHolder` to `onCreateViewHolder` significantly reduces object allocation during scrolling.
**Action:** Always set static listeners in `onCreateViewHolder` and use `bindingAdapterPosition` to retrieve the data item.

## 2024-05-23 - String Allocation in Scraping Loops
**Learning:** Calling `uppercase()` or `lowercase()` on strings inside tight loops (like Jsoup element iteration) creates significant GC pressure. Using `equals(..., ignoreCase = true)` is an O(1) allocation-free alternative.
**Action:** When filtering strings in a loop, pre-compute static comparison sets and use `equals(ignoreCase = true)` instead of normalizing the input string.
