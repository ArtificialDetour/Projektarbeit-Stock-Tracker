package com.project.stocktracker.controller;

import com.project.stocktracker.entity.User;
import org.springframework.security.core.Authentication;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.http.ResponseEntity;
import org.springframework.web.servlet.view.RedirectView;

/**
 * Renders the main server-side application pages.
 */
@Controller
@RequestMapping
public class PageController {

	/**
	 * Routes the root URL to the right first page for anonymous and authenticated users.
	 */
	@GetMapping({"/", "/home"})
	public RedirectView home(Authentication authentication) {
		if (authentication != null
				&& authentication.isAuthenticated()
				&& !(authentication instanceof AnonymousAuthenticationToken)) {
			return new RedirectView("/dashboard");
		}
		return new RedirectView("/login");
	}

	/**
	 * Injects the authenticated user's display name and email into the model
	 * for server-side rendering in the topnav fragment.
	 */
	private void addUserToModel(Authentication authentication, Model model) {
		if (authentication != null
				&& authentication.isAuthenticated()
				&& !(authentication instanceof AnonymousAuthenticationToken)
				&& authentication.getPrincipal() instanceof User user) {

			model.addAttribute("userName", user.getFullName());
			model.addAttribute("userEmail", user.getEmail());
		}
	}

	/**
	 * Renders the dashboard page.
	 */
	@GetMapping({"/dashboard", "/dashboard.html"})
	public String dashboard(Authentication authentication, Model model) {
		addUserToModel(authentication, model);
		model.addAttribute("activePage", "dashboard");
		return "dashboard";
	}

	/**
	 * Renders the portfolio page.
	 */
	@GetMapping({"/portfolio", "/portfolio.html"})
	public String portfolio(Authentication authentication, Model model) {
		addUserToModel(authentication, model);
		model.addAttribute("activePage", "portfolio");
		return "portfolio";
	}

	/**
	 * Renders the transaction page.
	 */
	@GetMapping({"/transaction", "/transaction.html"})
	public String transaction(Authentication authentication, Model model) {
		addUserToModel(authentication, model);
		model.addAttribute("activePage", "transaction");
		return "transaction";
	}

	/**
	 * Renders the automation page.
	 */
	@GetMapping({"/automation", "/automation.html"})
	public String automation(Authentication authentication, Model model) {
		addUserToModel(authentication, model);
		model.addAttribute("activePage", "automation");
		return "automation";
	}

	/**
	 * Renders the settings page.
	 */
	@GetMapping({"/settings", "/settings.html"})
	public String settings(Authentication authentication, Model model) {
		addUserToModel(authentication, model);
		model.addAttribute("activePage", "settings");
		return "settings";
	}

	/**
	 * Returns an empty favicon response.
	 */
	@GetMapping("/favicon.ico")
	public ResponseEntity<Void> favicon() {
		return ResponseEntity.noContent().build();
	}
}
