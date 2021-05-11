package sk.kasper.ui_launch


import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.asynclayoutinflater.view.AsyncLayoutInflater
import androidx.core.content.ContextCompat
import androidx.lifecycle.Observer
import androidx.navigation.fragment.findNavController
import androidx.navigation.ui.NavigationUI
import com.google.android.gms.common.ConnectionResult
import com.google.android.gms.common.GoogleApiAvailability
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.MapsInitializer
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MapStyleOptions
import com.google.android.gms.maps.model.MarkerOptions
import provideViewModel
import sk.kasper.domain.model.LaunchSite
import sk.kasper.space.launchdetail.gallery.GalleryAdapter
import sk.kasper.space.launchdetail.section.FalconInfoViewModel
import sk.kasper.ui_common.BaseFragment
import sk.kasper.ui_common.utils.*
import sk.kasper.ui_launch.databinding.FragmentLaunchBinding
import sk.kasper.ui_launch.databinding.FragmentLaunchRocketSectionBinding
import sk.kasper.ui_launch.gallery.PhotoPagerData
import sk.kasper.ui_launch.section.GalleryViewModel
import sk.kasper.ui_launch.section.LaunchSiteViewModel
import sk.kasper.ui_launch.section.OrbitViewModel
import sk.kasper.ui_launch.section.RocketSectionViewModel
import timber.log.Timber
import javax.inject.Inject


class LaunchFragment : BaseFragment() {

    private lateinit var launchSiteViewModel: LaunchSiteViewModel
    private lateinit var launchViewModel: LaunchViewModel
    private lateinit var binding: FragmentLaunchBinding

    @Inject
    lateinit var orbitViewModelFactory: OrbitViewModel.Factory

    @Inject
    lateinit var launchViewModelFactory: LaunchViewModel.Factory

    @Inject
    lateinit var falconInfoViewModelFactory: FalconInfoViewModel.Factory

    @Inject
    lateinit var galleryViewModelFactory: GalleryViewModel.Factory

    @Inject
    lateinit var rocketSectionViewModelFactory: RocketSectionViewModel.Factory

    @Inject
    lateinit var launchSiteViewModelFactory: LaunchSiteViewModel.Factory

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {

        binding = FragmentLaunchBinding.inflate(inflater, container, false)
        binding.tagsView.adapter = TagAdapter()
        binding.tagsView.addItemDecoration(HorizontalSpaceItemDecoration(R.dimen.launch_tag_horizontal_space.toPixels(requireContext())))

        requireActivity().window.statusBarColor = ContextCompat.getColor(requireContext(), R.color.systemUiOverlayColor)

        setupLaunchViewModel()
        setupRocketSection()
        setupLaunchSiteViewModel()
        setupGallery()
        setupOrbit()

        NavigationUI.setupWithNavController(binding.toolbar, findNavController())

        return binding.root
    }

    override fun onDestroyView() {
        super.onDestroyView()

        requireActivity().window.statusBarColor = android.R.attr.statusBarColor.getThemeColor(requireContext())
    }

    private fun openPhotoPager(photoPagerData: PhotoPagerData) {
        //findNavController().navigate(LaunchFragmentDirections.actionLaunchFragmentToPhotoPagerFragment(photoPagerData))
    }

    private fun setupGallery() {
        val viewModel: GalleryViewModel = provideViewModel {
            galleryViewModelFactory.create(
                getLaunchId()
            )
        }
        val galleryAdapter = GalleryAdapter(requireContext(), viewModel)
        binding.galleryRecyclerView.adapter = galleryAdapter

        viewModel.galleryItems.observe(viewLifecycleOwner, Observer {
            galleryAdapter.setItems(it)
        })

        viewModel.showPhotoPagerEvent.observe(viewLifecycleOwner, Observer {
            openPhotoPager(it)
        })

        binding.galleryViewModel = viewModel
    }

    private fun getLaunchId() = requireArguments().getString("launchId")!!

    private fun setupLaunchViewModel() {
        launchViewModel = provideViewModel { launchViewModelFactory.create(getLaunchId()) }
        binding.viewModel = launchViewModel
        launchViewModel.showVideoUrl.observe(viewLifecycleOwner, Observer {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(it))
            startActivity(intent)
        })

        /*binding.missionSectionComposeView.apply {
            setContent {
                SpaceTheme {
                    MissionSection(description = launchViewModel.description.observeAsState().value)
                }
            }
        }*/
    }

    private fun setupOrbit() {
        binding.orbitViewModel = provideViewModel { orbitViewModelFactory.create(getLaunchId()) }
    }

    private fun setupRocketSection() {
        AsyncLayoutInflater(requireContext()).inflate(R.layout.fragment_launch_rocket_section, binding.sectionsLinearLayout) { view, _, parent ->
            val rocketSectionViewModel: RocketSectionViewModel =
                provideViewModel { rocketSectionViewModelFactory.create(getLaunchId()) }

            val falconInfoViewModel: FalconInfoViewModel =
                provideViewModel { falconInfoViewModelFactory.create(getLaunchId()) }

            val fragmentLaunchRocketInfoBinding = FragmentLaunchRocketSectionBinding.bind(view)
            fragmentLaunchRocketInfoBinding.rocketSectionViewModel = rocketSectionViewModel
            fragmentLaunchRocketInfoBinding.falconInfoViewModel = falconInfoViewModel
            // to last position because of problem with gallery back animation
            parent?.addView(view, parent.childCount)
        }
    }

    private fun setupLaunchSiteViewModel() {
        launchSiteViewModel = provideViewModel { launchSiteViewModelFactory.create(
            getLaunchId(),
            ConnectionResult.SUCCESS == GoogleApiAvailability.getInstance()
                .isGooglePlayServicesAvailable(context)
        )
        }

        binding.mapView.onCreate(null)
        binding.mapView.getMapAsync { map ->
            MapsInitializer.initialize(context)
            map.uiSettings.isMapToolbarEnabled = false

            val success = map.setMapStyle(MapStyleOptions.loadRawResourceStyle(requireContext(), R.raw.map_style))
            if (!success) {
                Timber.e("Style parsing failed.")
            }

            binding.mapView.doOnApplyWindowInsets { insets, _ ->
                map.setPadding((insets.systemWindowInsetLeft + 8f.dp(requireContext())).toInt(), 0, 0, 0)
            }

            observeLaunchSiteViewModel(map, binding)
        }

        binding.launchSiteViewModel = launchSiteViewModel
    }

    private fun observeLaunchSiteViewModel(map: GoogleMap, binding: FragmentLaunchBinding) {
        launchSiteViewModel.launchSite.observe(viewLifecycleOwner, Observer { launchSite: LaunchSite ->
            val position = launchSite.position
            val latLng = LatLng(position.latitude, position.longitude)
            val cameraUpdate = CameraUpdateFactory.newLatLngZoom(latLng, 4f)
            map.moveCamera(cameraUpdate)
            map.addMarker(MarkerOptions()
                    .position(latLng)
                    .title(launchSite.name))

            binding.launchSiteInfo.text = launchSite.name
        })
    }
}
/*
@Composable
fun MissionSection(description: String?) {
    if (!description.isNullOrBlank()) {
        Column {
            Text(
                    text = stringResource(id = R.string.mission),
                    style = MaterialTheme.typography
                            .h6.copy(fontWeight = FontWeight.Medium),
                    modifier = Modifier
                            .heightIn(min = 48.dp)
                            .padding(top = 8.dp, bottom = 20.dp),
            )
            Text(
                    text = description,
                    style = MaterialTheme.typography.body1
            )
        }
    }
}

@Composable
@Preview
fun MissionSectionPreview() {
    SpaceTheme {
        MissionSection(description = "section text")
    }
}
 */