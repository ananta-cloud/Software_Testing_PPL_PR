package com.blog.controller;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
public class PostPageController {

	@RequestMapping("/page/index")
	public String getIndexPage() {
		return "index";
	}
	
	@RequestMapping(value="/page/detail/{id}")
	public String getIndexPage(@PathVariable("id") Long id, Model model) {
		model.addAttribute("id", id);
		return "detail";
	}
}
