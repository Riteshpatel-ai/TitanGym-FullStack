import ImageSlider from "./ImageSlider";

function ProductListHeader({ slideProducts }) {
    const src =
        "https://images.unsplash.com/photo-1599481238640-4c1288750d7a?ixlib=rb-4.0.3&ixid=MnwxMjA3fDB8MHxwaG90by1wYWdlfHx8fGVufDB8fHx8&auto=format&fit=crop&w=2664&q=80";

    return (
        <div className="bg-gradient-to-r from-lime-50 to-white text-slate-800 dark:from-slate-900 dark:to-slate-800 dark:text-slate-100 py-10 px-4">
            <div className="mx-auto flex max-w-7xl flex-col items-center justify-between gap-8 lg:flex-row">
                <div className="max-w-xl text-center lg:text-left">
                    <h1 className="text-4xl font-extrabold tracking-tight">
                        <span className="text-lime-600">TitanGym</span> Product Catalog
                    </h1>
                    <div className="mx-auto my-4 h-1 w-1/4 rounded-full bg-gradient-to-r from-lime-500 to-emerald-500 lg:mx-0" />
                    <p className="mt-2 text-slate-600 dark:text-slate-300">
                        Only gym-related products: strength gear, recovery essentials, training accessories,
                        and nutrition support.
                    </p>
                </div>

                <ImageSlider src={src} slideProducts={slideProducts} />
            </div>
        </div>
    );
}

export default ProductListHeader;
