## 2024-05-23 - RecyclerView Adapter Listener Optimization
**Learning:** Moving `setOnClickListener` from `onBindViewHolder` to `onCreateViewHolder` significantly reduces object allocation during scrolling.
**Action:** Always set static listeners in `onCreateViewHolder` and use `bindingAdapterPosition` to retrieve the data item.

## 2024-05-23 - String Allocation in Scraping Loops
**Learning:** Calling `uppercase()` or `lowercase()` on strings inside tight loops (like Jsoup element iteration) creates significant GC pressure. Using `equals(..., ignoreCase = true)` is an O(1) allocation-free alternative.
**Action:** When filtering strings in a loop, pre-compute static comparison sets and use `equals(ignoreCase = true)` instead of normalizing the input string.

## 2024-05-24 - Adapter Instantiation in Flow Collection
**Learning:** Instantiating `RecyclerView.Adapter` inside a Flow's `collect` block (or any observable callback) defeats the purpose of optimizations like `DiffUtil` because the adapter is completely replaced on every emission, forcing a full layout pass.
**Action:** Always instantiate adapters in `onCreate` (or `init`), assign them to the RecyclerView once, and only call `submitList` (or `notify...`) inside the data observation block.

## 2024-05-24 - URI Parsing Overhead in WebViews
**Learning:** `java.net.URI(string)` is surprisingly expensive (approx 1400ns vs 70ns for direct access) and should be avoided in hot paths like `WebViewClient.shouldInterceptRequest`. Android's `WebResourceRequest` already provides a pre-parsed `Uri` object.
**Action:** Always prefer accessing properties like `host` or `path` directly from `android.net.Uri` (or OkHttp `HttpUrl`) instead of converting to String and re-parsing.

## 2024-05-24 - Memoization of Finite Domain Checks
**Learning:** Iterating over a list of ~50 string keywords (O(N*M)) for every network request is wasteful when the set of visited domains is relatively small and repetitive.
**Action:** Use a `ConcurrentHashMap` to cache the boolean result of domain checks. This turns frequent checks into O(1) lookups, significantly reducing CPU usage in network-heavy apps (like those using WebViews).
